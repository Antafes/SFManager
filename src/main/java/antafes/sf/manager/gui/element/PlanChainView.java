/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sf.manager.gui.element;

import antafes.sf.manager.gui.util.MaterialIconLoader;
import antafes.sf.manager.service.dto.PlanMaterial;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * Draws a plan as a graph of step cards connected by labelled arrows.
 * Every arrow starts and ends at a port icon next to the cards and is routed through the gaps between the columns
 * or through corridors above and below the cards, so it never passes behind a card.
 */
public class PlanChainView extends JPanel
{
    private static final int MARGIN = 20;
    private static final int ROW_GAP = 40;
    private static final int OBSTACLE_PADDING = 6;
    private static final int CHANNEL_SPACING = 12;
    private static final int BRIDGE_RADIUS = 5;
    private static final int PORT_SIZE = 24;
    private static final int PORT_SPACE = PORT_SIZE + 4;
    private static final int PORT_SPACING = 30;
    private static final int PORT_ICON_SIZE = 18;
    private static final int LABEL_ICON_SIZE = 20;
    private static final int LABEL_AREA = 140;
    private static final int LANE_WIDTH = 12;
    private static final int LANE_PADDING = 14;
    private static final int CORRIDOR_STEP = 8;

    private final LanguageInterface language;
    private final String selectedLanguage;
    private final NumberFormat numberFormat;
    private final Map<String, JComponent> components = new HashMap<>();
    private List<Route> routes = List.of();

    public PlanChainView(LanguageInterface language, String selectedLanguage, NumberFormat numberFormat)
    {
        this.language = language;
        this.selectedLanguage = selectedLanguage;
        this.numberFormat = numberFormat;
        this.setLayout(null);
        this.installDragScrolling();
    }

    /**
     * Rebuilds the graph for the given result.
     *
     * @param stepNodeFactory creates the card of a production step
     */
    public void setResult(PlanResult result, Function<PlanStep, JComponent> stepNodeFactory)
    {
        this.removeAll();
        this.components.clear();
        this.routes = List.of();
        if (result == null) {
            this.setPreferredSize(new Dimension(0, 0));
            this.revalidate();
            this.repaint();
            return;
        }
        PlanChainLayout.Layout layout = PlanChainLayout.compute(result);
        List<PlanChainLayout.Edge> edges = layout.edges();
        Map<String, Integer> columns = new HashMap<>();
        int columnCount = layout.nodes().stream().mapToInt(PlanChainLayout.Node::column).max().orElse(0) + 1;
        int[] columnWidths = new int[columnCount];
        int[] columnHeights = new int[columnCount];
        for (PlanChainLayout.Node node : layout.nodes()) {
            JComponent component = this.createComponent(node, stepNodeFactory);
            this.components.put(node.id(), component);
            this.add(component);
            columns.put(node.id(), node.column());
            Dimension size = component.getPreferredSize();
            component.setSize(size);
            columnWidths[node.column()] = Math.max(columnWidths[node.column()], size.width);
            columnHeights[node.column()] += size.height + ROW_GAP;
        }

        // Every edge occupies one vertical lane in the gap behind its source and in the gap before its target.
        int[] lanesPerGap = new int[columnCount + 1];
        int[] outLane = new int[edges.size()];
        int[] inLane = new int[edges.size()];
        int corridorCount = 0;
        for (int i = 0; i < edges.size(); i++) {
            int outGap = columns.get(edges.get(i).fromId()) + 1;
            int inGap = columns.get(edges.get(i).toId());
            outLane[i] = lanesPerGap[outGap]++;
            if (inGap != outGap) {
                inLane[i] = lanesPerGap[inGap]++;
                corridorCount++;
            }
        }

        int[] gapX = new int[columnCount + 1];
        int[] columnX = new int[columnCount];
        int x = 0;
        for (int gap = 0; gap <= columnCount; gap++) {
            gapX[gap] = x;
            x += this.gapWidth(gap, columnCount, lanesPerGap[gap]);
            if (gap < columnCount) {
                columnX[gap] = x;
                x += columnWidths[gap];
            }
        }

        int totalHeight = 0;
        for (int height : columnHeights) {
            totalHeight = Math.max(totalHeight, height - ROW_GAP);
        }
        int verticalMargin = MARGIN + corridorCount * CORRIDOR_STEP;
        int[] nextY = new int[columnCount];
        for (int column = 0; column < columnCount; column++) {
            nextY[column] = verticalMargin + (totalHeight - (columnHeights[column] - ROW_GAP)) / 2;
        }
        for (PlanChainLayout.Node node : layout.nodes()) {
            JComponent component = this.components.get(node.id());
            int column = node.column();
            component.setLocation(columnX[column] + (columnWidths[column] - component.getWidth()) / 2, nextY[column]);
            nextY[column] += component.getHeight() + ROW_GAP;
        }
        int height = totalHeight + 2 * verticalMargin;

        this.routes = this.buildRoutes(edges, columns, gapX, lanesPerGap, outLane, inLane, verticalMargin, height);
        this.setPreferredSize(new Dimension(x, height));
        this.revalidate();
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Route route : this.routes) {
            this.paintRoute(g, route);
        }
        for (Route route : this.routes) {
            this.paintPort(g, route.edge(), route.outPort());
            this.paintPort(g, route.edge(), route.inPort());
        }
        for (Route route : this.routes) {
            this.paintLabel(g, route);
        }
        g.dispose();
    }

    private void installDragScrolling()
    {
        MouseAdapter adapter = new MouseAdapter()
        {
            private Point lastScreenPoint;

            @Override
            public void mousePressed(MouseEvent event)
            {
                this.lastScreenPoint = event.getLocationOnScreen();
                PlanChainView.this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                this.lastScreenPoint = null;
                PlanChainView.this.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent event)
            {
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                    JViewport.class,
                    PlanChainView.this
                );
                if (viewport == null || this.lastScreenPoint == null) {
                    return;
                }
                Point current = event.getLocationOnScreen();
                Point position = viewport.getViewPosition();
                int maxX = Math.max(0, PlanChainView.this.getWidth() - viewport.getWidth());
                int maxY = Math.max(0, PlanChainView.this.getHeight() - viewport.getHeight());
                position.x = Math.max(0, Math.min(maxX, position.x - (current.x - this.lastScreenPoint.x)));
                position.y = Math.max(0, Math.min(maxY, position.y - (current.y - this.lastScreenPoint.y)));
                viewport.setViewPosition(position);
                this.lastScreenPoint = current;
            }
        };
        this.addMouseListener(adapter);
        this.addMouseMotionListener(adapter);
    }

    private int gapWidth(int gap, int columnCount, int lanes)
    {
        int left = gap >= 1 ? PORT_SPACE + LABEL_AREA : MARGIN;
        int right = gap < columnCount ? PORT_SPACE : MARGIN;
        int laneArea = lanes > 0 ? lanes * LANE_WIDTH + LANE_PADDING : 0;

        return left + laneArea + right;
    }

    private int laneX(int[] gapX, int gap, int lane)
    {
        return gapX[gap] + (gap >= 1 ? PORT_SPACE + LABEL_AREA : MARGIN) + LANE_PADDING / 2 + lane * LANE_WIDTH;
    }

    private List<Route> buildRoutes(
        List<PlanChainLayout.Edge> edges,
        Map<String, Integer> columns,
        int[] gapX,
        int[] lanesPerGap,
        int[] outLane,
        int[] inLane,
        int verticalMargin,
        int height
    ) {
        Map<String, List<Integer>> outgoing = new HashMap<>();
        Map<String, List<Integer>> incoming = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            outgoing.computeIfAbsent(edges.get(i).fromId(), key -> new ArrayList<>()).add(i);
            incoming.computeIfAbsent(edges.get(i).toId(), key -> new ArrayList<>()).add(i);
        }
        Rectangle[] outPorts = new Rectangle[edges.size()];
        Rectangle[] inPorts = new Rectangle[edges.size()];
        for (Map.Entry<String, List<Integer>> entry : outgoing.entrySet()) {
            JComponent node = this.components.get(entry.getKey());
            List<Integer> indexes = entry.getValue();
            indexes.sort(Comparator.comparingDouble(i -> this.centerY(edges.get(i).toId())));
            for (int k = 0; k < indexes.size(); k++) {
                outPorts[indexes.get(k)] = new Rectangle(
                    node.getX() + node.getWidth() + 2,
                    this.portY(node, k, indexes.size()),
                    PORT_SIZE,
                    PORT_SIZE
                );
            }
        }
        for (Map.Entry<String, List<Integer>> entry : incoming.entrySet()) {
            JComponent node = this.components.get(entry.getKey());
            List<Integer> indexes = entry.getValue();
            indexes.sort(Comparator.comparingDouble(i -> this.centerY(edges.get(i).fromId())));
            for (int k = 0; k < indexes.size(); k++) {
                inPorts[indexes.get(k)] = new Rectangle(
                    node.getX() - 2 - PORT_SIZE,
                    this.portY(node, k, indexes.size()),
                    PORT_SIZE,
                    PORT_SIZE
                );
            }
        }

        List<Route> result = new ArrayList<>();
        List<Rectangle> obstacles = new ArrayList<>();
        for (JComponent component : this.components.values()) {
            Rectangle bounds = component.getBounds();
            bounds.grow(OBSTACLE_PADDING, OBSTACLE_PADDING);
            obstacles.add(bounds);
        }
        FontMetrics metrics = this.getFontMetrics(this.getFont());
        for (int i = 0; i < edges.size(); i++) {
            int labelWidth = LABEL_ICON_SIZE + 6 + metrics.stringWidth(this.labelText(edges.get(i))) + 8;
            obstacles.add(new Rectangle(
                outPorts[i].x,
                outPorts[i].y - 2,
                PORT_SIZE + 4 + labelWidth,
                PORT_SIZE + 4
            ));
            obstacles.add(new Rectangle(inPorts[i].x - 10, inPorts[i].y - 2, PORT_SIZE + 10, PORT_SIZE + 4));
        }
        List<int[]> channels = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            int outGap = columns.get(edges.get(i).fromId()) + 1;
            int inGap = columns.get(edges.get(i).toId());
            int laneOut = this.laneX(gapX, outGap, outLane[i]);
            int laneIn = outGap == inGap ? laneOut : this.laneX(gapX, inGap, inLane[i]);
            int startY = outPorts[i].y + PORT_SIZE / 2;
            int endY = inPorts[i].y + PORT_SIZE / 2;
            channels.add(new int[] {startY, outPorts[i].x + PORT_SIZE, laneOut, i});
            channels.add(new int[] {endY, laneIn, inPorts[i].x, i});
        }
        for (int i = 0; i < edges.size(); i++) {
            PlanChainLayout.Edge edge = edges.get(i);
            int outGap = columns.get(edge.fromId()) + 1;
            int inGap = columns.get(edge.toId());
            int startX = outPorts[i].x + PORT_SIZE;
            int startY = outPorts[i].y + PORT_SIZE / 2;
            int endX = inPorts[i].x;
            int endY = inPorts[i].y + PORT_SIZE / 2;
            int laneOut = this.laneX(gapX, outGap, outLane[i]);
            int[] xs;
            int[] ys;
            if (outGap == inGap) {
                xs = new int[] {startX, laneOut, laneOut, endX};
                ys = new int[] {startY, startY, endY, endY};
            } else {
                int laneIn = this.laneX(gapX, inGap, inLane[i]);
                int channelY = this.findChannel(
                    obstacles,
                    channels,
                    i,
                    Math.min(laneOut, laneIn),
                    Math.max(laneOut, laneIn),
                    startY,
                    endY,
                    height
                );
                channels.add(new int[] {channelY, Math.min(laneOut, laneIn), Math.max(laneOut, laneIn), i});
                xs = new int[] {startX, laneOut, laneOut, laneIn, laneIn, endX};
                ys = new int[] {startY, startY, channelY, channelY, endY, endY};
            }
            result.add(new Route(edge, xs, ys, outPorts[i], inPorts[i]));
        }

        return result;
    }

    /**
     * Finds the free horizontal channel between two lanes that is closest to the start and end height.
     */
    private int findChannel(
        List<Rectangle> obstacles,
        List<int[]> channels,
        int owner,
        int fromX,
        int toX,
        int startY,
        int endY,
        int height
    ) {
        List<Integer> candidates = new ArrayList<>(List.of(startY, endY));
        for (int y = CORRIDOR_STEP; y < height; y += CORRIDOR_STEP) {
            candidates.add(y);
        }
        int best = CORRIDOR_STEP / 2;
        int bestCost = Integer.MAX_VALUE;
        for (int y : candidates) {
            int cost = Math.abs(y - startY) + Math.abs(y - endY);
            if (cost >= bestCost || !this.isChannelFree(obstacles, channels, owner, fromX, toX, y)) {
                continue;
            }
            best = y;
            bestCost = cost;
        }

        return best;
    }

    private boolean isChannelFree(
        List<Rectangle> obstacles,
        List<int[]> channels,
        int owner,
        int fromX,
        int toX,
        int y
    ) {
        for (Rectangle obstacle : obstacles) {
            if (y >= obstacle.y && y <= obstacle.y + obstacle.height
                && obstacle.x < toX && obstacle.x + obstacle.width > fromX) {
                return false;
            }
        }
        for (int[] channel : channels) {
            if (channel[3] != owner && Math.abs(channel[0] - y) < CHANNEL_SPACING
                && channel[1] <= toX + 4 && channel[2] >= fromX - 4) {
                return false;
            }
        }

        return true;
    }

    private String labelText(PlanChainLayout.Edge edge)
    {
        return this.numberFormat.format(edge.rate()) + " / min";
    }

    private double centerY(String id)
    {
        JComponent component = this.components.get(id);

        return component.getY() + component.getHeight() / 2.0;
    }

    private int portY(JComponent node, int index, int count)
    {
        double center = node.getY() + node.getHeight() / 2.0;

        return (int) Math.round(center + (index - (count - 1) / 2.0) * PORT_SPACING - PORT_SIZE / 2.0);
    }

    /**
     * Builds the outline of a route and adds a small half circle wherever it crosses a vertical part of another route.
     */
    private Path2D buildPath(Route route)
    {
        Path2D.Double path = new Path2D.Double();
        int[] xs = route.xs();
        int[] ys = route.ys();
        path.moveTo(xs[0], ys[0]);
        for (int i = 0; i < xs.length - 1; i++) {
            if (ys[i] != ys[i + 1]) {
                path.lineTo(xs[i + 1], ys[i + 1]);
                continue;
            }
            int direction = Integer.compare(xs[i + 1], xs[i]);
            List<Integer> crossings = new ArrayList<>();
            for (Route other : this.routes) {
                if (other == route) {
                    continue;
                }
                for (int j = 0; j < other.xs().length - 1; j++) {
                    int x = other.xs()[j];
                    if (x != other.xs()[j + 1]) {
                        continue;
                    }
                    int minY = Math.min(other.ys()[j], other.ys()[j + 1]);
                    int maxY = Math.max(other.ys()[j], other.ys()[j + 1]);
                    if (ys[i] > minY && ys[i] < maxY && x > Math.min(xs[i], xs[i + 1])
                        && x < Math.max(xs[i], xs[i + 1])) {
                        crossings.add(x);
                    }
                }
            }
            crossings.sort(Comparator.naturalOrder());
            int minX = Math.min(xs[i], xs[i + 1]);
            int maxX = Math.max(xs[i], xs[i + 1]);

            // Crossings that are too close for separate half circles share one wider half circle.
            List<int[]> bridges = new ArrayList<>();
            for (int x : crossings) {
                int low = Math.max(minX, x - BRIDGE_RADIUS);
                int high = Math.min(maxX, x + BRIDGE_RADIUS);
                int[] last = bridges.isEmpty() ? null : bridges.getLast();
                if (last != null && low - last[1] < 2) {
                    last[1] = high;
                } else {
                    bridges.add(new int[] {low, high});
                }
            }
            if (direction < 0) {
                Collections.reverse(bridges);
            }
            for (int[] bridge : bridges) {
                path.lineTo(direction > 0 ? bridge[0] : bridge[1], ys[i]);
                path.append(
                    new Arc2D.Double(
                        bridge[0],
                        ys[i] - BRIDGE_RADIUS,
                        bridge[1] - bridge[0],
                        BRIDGE_RADIUS * 2,
                        direction > 0 ? 180 : 0,
                        direction > 0 ? -180 : 180,
                        Arc2D.OPEN
                    ),
                    true
                );
            }
            path.lineTo(xs[i + 1], ys[i + 1]);
        }

        return path;
    }

    private void paintRoute(Graphics2D g, Route route)
    {
        PlanChainLayout.Edge edge = route.edge();
        g.setColor(edge.dashed() ? Color.GRAY : Color.DARK_GRAY);
        g.setStroke(edge.dashed()
            ? new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[] {6f, 4f}, 0f)
            : new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(this.buildPath(route));
        g.setStroke(new BasicStroke(1f));
        int endX = route.xs()[route.xs().length - 1];
        int endY = route.ys()[route.ys().length - 1];
        g.fillPolygon(new int[] {endX, endX - 8, endX - 8}, new int[] {endY, endY - 5, endY + 5}, 3);
    }

    private void paintPort(Graphics2D g, PlanChainLayout.Edge edge, Rectangle port)
    {
        g.setColor(Color.WHITE);
        g.fillRoundRect(port.x, port.y, port.width, port.height, 8, 8);
        g.setColor(edge.dashed() ? Color.GRAY : Color.DARK_GRAY);
        g.drawRoundRect(port.x, port.y, port.width, port.height, 8, 8);
        MaterialIconLoader.load(edge.item().getIcon(), PORT_ICON_SIZE).paintIcon(
            this,
            g,
            port.x + (port.width - PORT_ICON_SIZE) / 2,
            port.y + (port.height - PORT_ICON_SIZE) / 2
        );
    }

    private void paintLabel(Graphics2D g, Route route)
    {
        String text = this.labelText(route.edge());
        FontMetrics metrics = g.getFontMetrics();
        int width = LABEL_ICON_SIZE + 6 + metrics.stringWidth(text) + 8;
        int height = LABEL_ICON_SIZE + 4;
        int x = route.xs()[0] + 4;
        int y = route.ys()[0] - height / 2;
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(Color.GRAY);
        g.drawRoundRect(x, y, width, height, 8, 8);
        MaterialIconLoader.load(route.edge().item().getIcon(), LABEL_ICON_SIZE).paintIcon(this, g, x + 3, y + 2);
        g.setColor(Color.BLACK);
        g.drawString(text, x + LABEL_ICON_SIZE + 6, y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private JComponent createComponent(PlanChainLayout.Node node, Function<PlanStep, JComponent> stepNodeFactory)
    {
        return switch (node.kind()) {
            case STEP, SIDE_STEP -> stepNodeFactory.apply(node.step());
            case RAW -> this.createMaterialNode(node.material(), true);
            case TARGET_BYPRODUCT -> this.createMaterialNode(node.material(), false);
        };
    }

    private JPanel createMaterialNode(PlanMaterial material, boolean raw)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        JLabel icon = new JLabel(MaterialIconLoader.load(material.item().getIcon(), 48));
        JLabel name = new JLabel(material.item().getName() == null
            ? material.item().getKey()
            : material.item().getName().getLocalizedName(this.selectedLanguage));
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        JLabel rate = new JLabel(this.numberFormat.format(material.rate()) + " / min");
        panel.add(icon);
        panel.add(name);
        panel.add(rate);
        if (raw && material.available() != 0) {
            panel.add(new JLabel(
                this.language.translate("planAvailable") + ": " + this.numberFormat.format(material.available())
            ));
        } else if (!raw) {
            panel.add(new JLabel(this.language.translate("planTargetByproduct")));
        }
        for (Component component : panel.getComponents()) {
            ((JComponent) component).setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        return panel;
    }

    private record Route(PlanChainLayout.Edge edge, int[] xs, int[] ys, Rectangle outPort, Rectangle inPort)
    {
    }
}
