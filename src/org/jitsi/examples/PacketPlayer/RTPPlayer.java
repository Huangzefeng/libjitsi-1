package org.jitsi.examples.PacketPlayer;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.List;
import java.util.logging.*;

import javax.sdp.*;
import javax.swing.*;
import javax.swing.table.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.impl.neomedia.device.AudioSystem.DataFlow;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.format.*;

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
        System.setProperty("java.util.logging.config.file",
            "logging.properties");
        try
        {
            LogManager.getLogManager().readConfiguration();
        }
        catch (SecurityException e1)
        {
            e1.printStackTrace();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

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

    private JComboBox<String> codecComboBox;
    private JComboBox<String> audioDeviceComboBox;
    private JComboBox<Boolean> autoComboBox;
    private JComboBox<Boolean> stopOnNoAudioComboBox;

    public void playRow(int row)
    {
        final int ssrc = (Integer) (rows.get(row)[1]);
        List<Byte> payloadList = (List<Byte>) rows.get(row)[3];
        final byte initialPT = payloadList.get(0);
        final List<Byte> dynamicPayloadTypes = new LinkedList<Byte>();
        for (byte pt : payloadList)
        {
            if (pt > 34)
            {
                dynamicPayloadTypes.add(pt);
            }
        }

        Thread myThead = new Thread()
        {

            @Override
            public void run()
            {
            	PlayRTP playRTP = new PlayRTP(); // Also initializes Libjitsi

                // Set the appropriate output device
	            String selectedDeviceStr = (String)audioDeviceComboBox.getSelectedItem();
	            CaptureDeviceInfo2 selectedDevice = null;
	            AudioSystem audioSystem = ((MediaServiceImpl)LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
	            for (CaptureDeviceInfo2 device : audioSystem.getDevices(DataFlow.PLAYBACK)) {
	            	if (device.getName().equals(selectedDeviceStr)) {
	            		selectedDevice = device;
	            		break;
	            	}
	            }
	            System.out.println((selectedDevice == null) ? "Couldn't find output device." : "Selected device: " + selectedDevice.getName());
	            audioSystem.setDevice(DataFlow.PLAYBACK, selectedDevice, true);

                // Get the codec we should use for dynamic payload types from
                // the drop down box.
                String codec =
                    ((String) codecComboBox.getSelectedItem()).split("/")[0];
                double frequency = Double.parseDouble(
                    ((String) codecComboBox.getSelectedItem()).split("/")[1]);
                MediaFormat dynamicFormat = LibJitsi.getMediaService()
                    .getFormatFactory().createMediaFormat(codec, frequency);

                // Set the initial format of this stream from the initial
                // payload type - it's either a standard payload type or the
                // same as the dynamic format we just calculated.
                MediaFormat initialFormat = dynamicFormat;
                if (initialPT <= 34)
                {
                    codec = SdpConstants.avpTypeNames[initialPT];
                    initialFormat = LibJitsi.getMediaService()
                        .getFormatFactory().createMediaFormat(codec,
                            (double)8000); // g711 and 722 using 8K always
                }

                playRTP.playFile(lblFileName.getText(), initialFormat,
                        dynamicPayloadTypes, dynamicFormat, ssrc, 10);
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
        springLayout.putConstraint(SpringLayout.NORTH, lblCurrentFile, 2,
            SpringLayout.SOUTH, btnChooseFile);
        springLayout.putConstraint(SpringLayout.WEST, lblCurrentFile, 0,
            SpringLayout.WEST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, lblCurrentFile, 113,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().add(lblCurrentFile);

        lblFileName = new JLabel("");
        springLayout.putConstraint(SpringLayout.NORTH, lblFileName, 2,
            SpringLayout.SOUTH, lblCurrentFile);
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

        // Table is added to view after the other items so it can be located properly

        codecComboBox = new JComboBox<String>();
        codecComboBox.setModel(new DefaultComboBoxModel<String>(new String[]
        { "SILK/8000", "SILK/16000","H264/90000" }));
        springLayout.putConstraint(SpringLayout.NORTH, codecComboBox, 0,
            SpringLayout.NORTH, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, codecComboBox, -152,
            SpringLayout.EAST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, codecComboBox, 0,
            SpringLayout.EAST, mframe.getContentPane());
        mframe.getContentPane().add(codecComboBox);

        JLabel lblAssumeCodecFor = new JLabel("Assume codec for dynamic PT");
        springLayout.putConstraint(SpringLayout.NORTH, lblAssumeCodecFor, 0,
            SpringLayout.NORTH, btnChooseFile);
        springLayout.putConstraint(SpringLayout.EAST, lblAssumeCodecFor, -10,
            SpringLayout.WEST, codecComboBox);
        mframe.getContentPane().add(lblAssumeCodecFor);

        // Choose the output audio device
        LibJitsi.start();
        MediaService mediaService = LibJitsi.getMediaService();
        final AudioSystem audioSystem = ((MediaServiceImpl)mediaService).getDeviceConfiguration().getAudioSystem();
        String [] deviceList = audioSystem.getAllDevices(DataFlow.PLAYBACK);
        LibJitsi.stop();

        audioDeviceComboBox = new JComboBox<>();
        audioDeviceComboBox.setModel(new DefaultComboBoxModel<>(deviceList));
        springLayout.putConstraint(SpringLayout.NORTH, audioDeviceComboBox, 0,
                SpringLayout.SOUTH, codecComboBox);
        springLayout.putConstraint(SpringLayout.EAST, audioDeviceComboBox, 0,
                SpringLayout.EAST, mframe.getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, audioDeviceComboBox, -152,
        		SpringLayout.EAST, audioDeviceComboBox);
        mframe.getContentPane().add(audioDeviceComboBox);

        JLabel lblAudioDevice = new JLabel("Audio device:");
        springLayout.putConstraint(SpringLayout.NORTH, lblAudioDevice, 0,
            SpringLayout.NORTH, audioDeviceComboBox);
        springLayout.putConstraint(SpringLayout.EAST, lblAudioDevice, -10,
            SpringLayout.WEST, audioDeviceComboBox);
        mframe.getContentPane().add(lblAudioDevice);

        // Automation settings. Is auto-mode on? How many iterations? Stop on no audio?
        JLabel lblAutoHeading = new JLabel("Parameters for automatic mode:");
        springLayout.putConstraint(SpringLayout.NORTH, lblAutoHeading, 6,
            SpringLayout.SOUTH, lblFileName);
        springLayout.putConstraint(SpringLayout.WEST, lblAutoHeading, 2,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().add(lblAutoHeading);
        
        // 1) Is automation mode on? lblAuto, autoComboBox (true/false)
        JLabel lblAuto = new JLabel("Run in auto-mode?");
        springLayout.putConstraint(SpringLayout.NORTH, lblAuto, 6,
            SpringLayout.SOUTH, lblAutoHeading);
        springLayout.putConstraint(SpringLayout.WEST, lblAuto, 2,
            SpringLayout.WEST, mframe.getContentPane());
        mframe.getContentPane().add(lblAuto);
        
        autoComboBox = new JComboBox<>();
        autoComboBox.setModel(new DefaultComboBoxModel<>(new Boolean[]
        		                                                 {false,true}));
        springLayout.putConstraint(SpringLayout.NORTH, autoComboBox, 0,
                SpringLayout.NORTH, lblAuto);
        springLayout.putConstraint(SpringLayout.WEST, autoComboBox, 2,
                SpringLayout.EAST, lblAuto);
        springLayout.putConstraint(SpringLayout.EAST, autoComboBox, 55,
        		SpringLayout.WEST, autoComboBox);
        mframe.getContentPane().add(autoComboBox);
        
        // 2) How many iterations should we run? lblAutoIters[2], autoItersInput
        JLabel lblIters = new JLabel("N iterations");
        springLayout.putConstraint(SpringLayout.NORTH, lblIters, 0,
                SpringLayout.NORTH, lblAuto);
        springLayout.putConstraint(SpringLayout.WEST, lblIters, 10,
                SpringLayout.EAST, autoComboBox);
        mframe.getContentPane().add(lblIters);
        JLabel lblIters2 = new JLabel("(0=infinite)");
        springLayout.putConstraint(SpringLayout.NORTH, lblIters2, 2,
                SpringLayout.SOUTH, lblIters);
            springLayout.putConstraint(SpringLayout.WEST, lblIters2, 2,
                SpringLayout.WEST, lblIters);
            mframe.getContentPane().add(lblIters2);
            
        JTextField autoItersInput = new JTextField();
        autoItersInput.setText("1");
        springLayout.putConstraint(SpringLayout.NORTH, autoItersInput, 0,
                SpringLayout.NORTH, lblIters);
        springLayout.putConstraint(SpringLayout.WEST, autoItersInput, 2,
                SpringLayout.EAST, lblIters);
        springLayout.putConstraint(SpringLayout.EAST, autoItersInput, 60,
            		SpringLayout.WEST, autoItersInput);
        mframe.getContentPane().add(autoItersInput);
        
        // 3) Stop if an iteration has no audio? lblAutoStopOnNoAudio[2], stopOnNoAudioComboBox
        JLabel lblAutoStopOnNoAudio = new JLabel("Stop if iter");
        springLayout.putConstraint(SpringLayout.NORTH, lblAutoStopOnNoAudio, 0,
                SpringLayout.NORTH, lblAuto);
        springLayout.putConstraint(SpringLayout.WEST, lblAutoStopOnNoAudio, 10,
                SpringLayout.EAST, autoItersInput);
        mframe.getContentPane().add(lblAutoStopOnNoAudio);
        JLabel lblAutoStopOnNoAudio2 = new JLabel("has no audio");
        springLayout.putConstraint(SpringLayout.NORTH, lblAutoStopOnNoAudio2, 2,
                SpringLayout.SOUTH, lblAutoStopOnNoAudio);
        springLayout.putConstraint(SpringLayout.WEST, lblAutoStopOnNoAudio2, 2,
                SpringLayout.WEST, lblAutoStopOnNoAudio);
        mframe.getContentPane().add(lblAutoStopOnNoAudio2);
        
        stopOnNoAudioComboBox = new JComboBox<>();
        stopOnNoAudioComboBox.setModel(new DefaultComboBoxModel<>(new Boolean[]
        		                                                 {true,false}));
        springLayout.putConstraint(SpringLayout.NORTH, stopOnNoAudioComboBox, 0,
                SpringLayout.NORTH, lblAuto);
        springLayout.putConstraint(SpringLayout.WEST, stopOnNoAudioComboBox, 2,
                SpringLayout.EAST, lblAutoStopOnNoAudio2);
        springLayout.putConstraint(SpringLayout.EAST, stopOnNoAudioComboBox, 55,
        		SpringLayout.WEST, stopOnNoAudioComboBox);
        mframe.getContentPane().add(stopOnNoAudioComboBox);

        // Now add the table of streams.
        JScrollPane scrollPane_1 = new JScrollPane();
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane_1, 10,
            SpringLayout.SOUTH, autoComboBox);
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

            rows.add(new Object[]
                { String.format("0x%08X", ssrc), ssrc, packets, ptList });

            System.out.println(String.format("0x%08x = %s packets\n PTs: %s",
                ssrc, packets, ptList));
        }
        myTable.fireTableDataChanged();
    }
}
