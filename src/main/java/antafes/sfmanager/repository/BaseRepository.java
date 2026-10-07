/*
 * This file is part of CargoLibrary.
 *
 * CargoLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CargoLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CargoLibrary. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package CargoLibrary
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2024, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sfmanager.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.Map;

abstract class BaseRepository<T, K>
{
    protected final Map<K, T> entities = new HashMap<>();
    @Autowired
    protected ResourceLoader resourceLoader;
    private boolean dataLoaded = false;

    public void init()
    {
        if (!this.dataLoaded) {
            this.loadData();
            this.dataLoaded = true;
        }
    }

    public Map<K, T> findAll()
    {
        return this.entities;
    }

    public T findOne(K key)
    {
        return this.entities.get(key);
    }

    abstract protected void loadData();
}
