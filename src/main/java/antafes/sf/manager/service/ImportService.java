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

package antafes.sf.manager.service;

import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.repository.ProductionRepository;
import antafes.sf.manager.service.dto.ImportResult;
import antafes.sf.manager.service.exception.UnsupportedSaveVersionException;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import antafes.sf.manager.service.event.ProductionSavedEvent;
import antafes.sf.manager.service.mapper.SaveProductionMapper;
import antafes.sf.saveparser.SaveParserService;
import antafes.sf.saveparser.dto.ParsedSave;
import antafes.sf.saveparser.error.SaveParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Imports factories from Satisfactory save files.
 */
@Service
public class ImportService
{
    // The parser returns empty object data for this save version, so no machine settings can be read.
    private static final long SAVE_VERSION_WITHOUT_OBJECT_DATA = 36;

    @Autowired
    private SaveParserService saveParserService;
    @Autowired
    private SaveProductionMapper saveProductionMapper;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Parses the save file and maps its machines. Nothing is persisted.
     *
     * @throws UnsupportedSaveVersionException if the save version does not contain machine data
     * @throws IOException if the file cannot be read or is not a valid save
     */
    public ImportResult readSave(Path path) throws IOException
    {
        try {
            return this.mapSave(this.saveParserService.readFullSaveFile(path));
        } catch (SaveParseException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    /**
     * Parses the save stream and maps its machines. Nothing is persisted.
     *
     * @throws UnsupportedSaveVersionException if the save version does not contain machine data
     * @throws IOException if the stream cannot be read or is not a valid save
     */
    public ImportResult readSave(InputStream inputStream) throws IOException
    {
        try {
            return this.mapSave(this.saveParserService.readFullSaveFile(inputStream));
        } catch (SaveParseException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    /**
     * Creates a factory with the imported productions. If any step fails, the new factory and its
     * productions are removed again and no events are published.
     *
     * @throws IllegalArgumentException if the name is invalid, already used, or there is nothing to import
     * @throws IOException if persisting fails
     */
    public Factory importFactory(ImportResult result, String name) throws IOException
    {
        if (result == null || result.productions().isEmpty()) {
            throw new IllegalArgumentException("The save contains no productions that can be imported.");
        }

        Factory factory = this.factoryService.createFactory(name);
        List<Production> productions = result.productions();
        productions.forEach(production -> production.setId(UUID.randomUUID()));
        List<UUID> productionIds = productions.stream().map(Production::getId).toList();
        try {
            productions.forEach(production -> factory.getProductions().add(production.getId().toString()));
            this.productionRepository.saveProductions(factory, productions);
            this.factoryService.saveFactory(factory);
        } catch (IOException | RuntimeException failure) {
            this.rollback(factory, productionIds, failure);
            throw failure;
        }

        this.eventPublisher.publishEvent(new FactorySelectedEvent(factory));
        productions.forEach(production -> this.eventPublisher.publishEvent(new ProductionSavedEvent(production)));
        return factory;
    }

    private ImportResult mapSave(ParsedSave save) throws UnsupportedSaveVersionException
    {
        long saveVersion = save.fileInfo().saveVersion();

        if (saveVersion == SAVE_VERSION_WITHOUT_OBJECT_DATA) {
            throw new UnsupportedSaveVersionException(saveVersion);
        }

        return this.saveProductionMapper.map(save);
    }

    private void rollback(Factory factory, List<UUID> productionIds, Exception failure)
    {
        factory.getProductions().clear();
        try {
            this.productionRepository.deleteProductions(factory, productionIds);
        } catch (IOException | RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
        try {
            this.factoryService.deleteFactory(factory);
        } catch (IOException | RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
