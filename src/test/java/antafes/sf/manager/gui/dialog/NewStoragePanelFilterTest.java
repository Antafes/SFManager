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

package antafes.sf.manager.gui.dialog;

import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.service.dto.StorageProductionChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class NewStoragePanelFilterTest
{
    @Test
    public void testFilterChoicesByItem()
    {
        Item iron = new Item().setKey("iron");
        Item copper = new Item().setKey("copper");
        StorageProductionChoice ironChoice = new StorageProductionChoice(new Production(), iron, 1);
        StorageProductionChoice copperChoice = new StorageProductionChoice(new Production(), copper, 2);
        List<StorageProductionChoice> choices = List.of(ironChoice, copperChoice);

        assertSame(choices, NewStoragePanel.filterChoices(choices, null));
        List<StorageProductionChoice> filtered = NewStoragePanel.filterChoices(choices, new Item().setKey("copper"));
        assertEquals(1, filtered.size());
        assertSame(copperChoice, filtered.getFirst());
    }
}
