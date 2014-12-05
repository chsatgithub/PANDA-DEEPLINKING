package de.fuberlin.panda.data.configuration.resourcemap.creator;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universität Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Dialog to save PANDA resource map configuration.
 * 
 * @author Christoph Schröder
 */
public class SaveDialog extends JFileChooser {

    /**
     * 
     */
    private static final long serialVersionUID = 3977134573193430806L;

    public SaveDialog() {
        FileFilter configFileFilter = new FileNameExtensionFilter("PANDA XML Config File", "xml");
        this.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.setAcceptAllFileFilterUsed(false);
        this.setFileFilter(configFileFilter);
        this.setSize(300, 200);
    }
}
