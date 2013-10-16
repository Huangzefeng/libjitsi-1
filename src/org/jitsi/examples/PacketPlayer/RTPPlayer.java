package org.jitsi.examples.PacketPlayer;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.List;

import javax.sdp.*;
import javax.swing.*;
import javax.swing.table.*;

public class RTPPlayer
{
    private final class TableMouseListener
        implements MouseListener
    {
        @Override
        public void mouseClicked(MouseEvent event)
        {
            int row = mtable.rowAtPoint(event.getPoint());
            int col = mtable.columnAtPoint(event.getPoint());

            if (col == 4)
            {
                // This is the play column
                playRow(row);
            }

        }

        @Override
        public void mouseEntered(MouseEvent e){}

        @Override
        public void mouseExited(MouseEvent e){}

        @Override
        public void mousePressed(MouseEvent e){}

        @Override
        public void mouseReleased(MouseEvent e){}
    }

    private final JFrame mframe = new JFrame();
    private JFileChooser fc;
    private JLabel lblFileName;
    private JTable mtable;
    private AbstractTableModel myTable;

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {

        final String possibleInputFile = args.length > 0 ? args[0] : "";

        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                try
                {
                    RTPPlayer window = new RTPPlayer();
                    window.mframe.setVisible(true);
                    if (new File(possibleInputFile).exists())
                    {
                        window.loadFile(new File(possibleInputFile));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    final PlayRTP playRTP = new PlayRTP();

    private JComboBox comboBox;

    public void playRow(int row)
    {
        final int ssrc = (Integer) (rows.get(row)[1]);
        final byte pt = (Byte) rows.get(row)[3];
        Thread myThead = new Thread()
        {

            @Override
            public void run()
            {
                String codec = "";
                double frequency = 8000; // g711 and 722 using 8K always
                if (pt > 34)
                {
                    codec = ((String) comboBox.getSelectedItem()).split("/")[0];
                    frequency =
                        Double
                            .parseDouble(((String) comboBox.getSelectedItem())
                                .split("/")[1]);
                }
                else
                {
                    codec = SdpConstants.avpTypeNames[pt];
                }

                playRTP.playFile(lblFileName.getText(), codec, frequency, pt,
                    ssrc);
            }

        };

        myThead.start();
    }

    /**
     * Create the application.
     */
    public RTPPlayer()
    {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        mframe.setBounds(100, 100, 517, 366);
        mframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JButton btnChooseFile = new JButton("Choose File");
        btnChooseFile.setDropTarget(new DropTarget() {
        	public synchronized void drop(DropTargetDropEvent evt) {
        		try {
        			evt.acceptDrop(DnDConstants.ACTION_COPY);
        			List<File> droppedFiles = (List<File>)
        					evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

        			if (droppedFiles.size() == 1)
        			{
        				loadFile(droppedFiles.get(0));
        			}

        		} catch (Exception ex) {
        			ex.printStackTrace();
        		}
        	}
        });
        btnChooseFile.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(mframe);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File file = fc.getSelectedFile();
                    System.out.println("Got " + file);
                    if (file.exists())
                    {

                        loadFile(file);
                    }
                }
            }
        });
        SpringLayout springLayout = new SpringLayout();
        springLayout.putConstraint(SpringLayout.NORTH, btnChooseFile, 0,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, btnChooseFile, 0,
            SpringLayout.WEST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, btnChooseFile, 137,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().setLayout(springLayout);
        mframe.getContentPane().add(btnChooseFile);

        JLabel lblCurrentFile = new JLabel("Current File:");
        springLayout.putConstraint(SpringLayout.NORTH, lblCurrentFile, 24,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, lblCurrentFile, 0,
            SpringLayout.WEST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, lblCurrentFile, 113,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().add(lblCurrentFile);

        lblFileName = new JLabel("");
        springLayout.putConstraint(SpringLayout.NORTH, lblFileName, 34,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, lblFileName, 10,
            SpringLayout.WEST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, lblFileName, 434,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().add(lblFileName);

        myTable = new AbstractTableModel()
        {
            String[] columnNames =
            { "SSRC", "SSRC", "Packets", "PT", "Play" };

            public String getColumnName(int col)
            {
                return columnNames[col].toString();
            }

            public int getRowCount()
            {
                return rows.size();
            }

            public int getColumnCount()
            {
                return columnNames.length;
            }

            public Object getValueAt(int row, int col)
            {
                if (col == 4)
                {
                    return "Click to Play";
                }
                else if (col == 0)
                {
                    return String.format("0x%08x", rows.get(row)[col]);
                }
                else
                {
                    return rows.get(row)[col];
                }
            }

            public boolean isCellEditable(int row, int col)
            {
                return false;
            }

            public void setValueAt(Object value, int row, int col)
            {
            }
        };

        JScrollPane scrollPane_1 = new JScrollPane();
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane_1, 49,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, scrollPane_1, 0,
            SpringLayout.WEST, btnChooseFile);
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane_1, 0,
            SpringLayout.SOUTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, scrollPane_1, 0,
            SpringLayout.EAST, mframe.getContentPane());
        mframe.getContentPane().add(scrollPane_1);

        mtable = new JTable(myTable);
        mtable.addMouseListener(new TableMouseListener());
        scrollPane_1.setViewportView(mtable);
        mtable.setRowSelectionAllowed(false);

        comboBox = new JComboBox();
        comboBox.setModel(new DefaultComboBoxModel(new String[]
        { "SILK/8000", "SILK/16000" }));
        springLayout.putConstraint(SpringLayout.NORTH, comboBox, 0,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, comboBox, -152,
            SpringLayout.EAST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, comboBox, 0,
            SpringLayout.EAST, mframe.getContentPane());
        mframe.getContentPane().add(comboBox);

        JLabel lblAssumeCodecFor = new JLabel("Assume codec for dynamic PT");
        springLayout.putConstraint(SpringLayout.NORTH, lblAssumeCodecFor, 0,
            SpringLayout.NORTH, btnChooseFile);
        springLayout.putConstraint(SpringLayout.EAST, lblAssumeCodecFor, -10,
            SpringLayout.WEST, comboBox);
        mframe.getContentPane().add(lblAssumeCodecFor);
    }

    ArrayList<Object[]> rows = new ArrayList<Object[]>();

    protected void loadFile(File file)
    {
        lblFileName.setText(file.toString());
        rows.clear();

        StreamIdentifier streamIdentifier =
            new StreamIdentifier(file.toString());
        for (Entry<Integer, Integer> entry : streamIdentifier.ssrcPacketCounts
            .entrySet())
        {
            int ssrc = entry.getKey();
            Integer packets = entry.getValue();
            List<Byte> ptList =
                streamIdentifier.ssrcPayloadTypes.get(entry.getKey());

            for (Byte aPt : ptList)
            {
                rows.add(new Object[]
                { String.format("%X", ssrc), ssrc, packets, aPt });
            }

            System.out.println(String.format("0x%08x = %s packets\n PTs: %s",
                ssrc, packets, ptList));
        }
        myTable.fireTableDataChanged();
    }
}
