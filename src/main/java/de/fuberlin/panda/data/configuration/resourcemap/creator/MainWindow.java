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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Main window of the resource map configuration creator GUI tool.
 * 
 * @author Christoph Schröder
 */
public class MainWindow extends JFrame {

    /**
     * 
     */
    private static final long  serialVersionUID        = -6669002555086590394L;

    // Menu Bar
    JMenu                      fileMenu                = new JMenu("Files");
    JMenuItem                  openItem                = new JMenuItem("Open");
    JMenuItem                  saveItem                = new JMenuItem("Save");
    JMenuItem                  saveAsItem              = new JMenuItem("Save as...");
    JMenuItem                  closeItem               = new JMenuItem("Close");

    // Buttons
    JButton                    openButton              = new JButton("Open");
    JButton                    saveButton              = new JButton("Save");
    JButton                    saveAsButton            = new JButton("Save as...");
    JButton                    addLocalFileButton      = new JButton("Add Local File");
    JButton                    addOnlineResourceButton = new JButton("New Resources");
    JButton                    deleteSelectedButton    = new JButton("Delete Selected");

    // Panel and menu
    JPanel                     tablePanel              = new JPanel(new BorderLayout());
    JPanel                     menuPanel               = new JPanel(new BorderLayout());
    JPanel                     buttonPanel1            = new JPanel(new GridLayout());
    JPanel                     buttonPanel2            = new JPanel(new GridLayout());
    JMenuBar                   menuBar                 = new JMenuBar();

    // Table
    ResourceTableModel         tableModel              = new ResourceTableModel();
    JTable                     resourceTable           = new JTable(tableModel);
    TableRowSorter<TableModel> rowSorter               = new TableRowSorter<TableModel>(tableModel);
    JScrollPane                tableScrollPane         = new JScrollPane(resourceTable);

    public MainWindow(ActionListener actionListener) {

        // Set up frame
        this.setTitle("PANDA Resource Map Creator");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        // Add Listener to Buttons
        openButton.addActionListener(actionListener);
        saveButton.addActionListener(actionListener);
        saveAsButton.addActionListener(actionListener);
        addLocalFileButton.addActionListener(actionListener);
        addOnlineResourceButton.addActionListener(actionListener);
        deleteSelectedButton.addActionListener(actionListener);

        // Add Listener to Menu Items
        openItem.addActionListener(actionListener);
        saveItem.addActionListener(actionListener);
        saveAsItem.addActionListener(actionListener);
        closeItem.addActionListener(actionListener);

        // Add buttons to Panel
        buttonPanel1.add(openButton);
        buttonPanel1.add(saveButton);
        buttonPanel1.add(saveAsButton);
        buttonPanel2.add(addLocalFileButton);
        buttonPanel2.add(addOnlineResourceButton);
        buttonPanel2.add(deleteSelectedButton);

        // Configure and add Table
        resourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resourceTable.setFillsViewportHeight(true);
        resourceTable.setRowSelectionAllowed(true);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        resourceTable.setRowSorter(rowSorter);

        // set dimensions
        this.setMinimumSize(new Dimension(640, 400));
        this.setSize(new Dimension(640, 400));
        resourceTable.setPreferredScrollableViewportSize(new Dimension(620, 280));

        // Add menu items to menu
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(closeItem);

        // Add menu to menu bar
        menuBar.add(fileMenu);

        // Add panels and menu to main frame
        menuPanel.add(menuBar, BorderLayout.NORTH);
        menuPanel.add(buttonPanel1, BorderLayout.SOUTH);
        this.getContentPane().add(menuPanel, BorderLayout.NORTH);
        this.getContentPane().add(tablePanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel2, BorderLayout.SOUTH);

        // Center Frame
        this.setLocationRelativeTo(null);
    }

}
