import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;

/*
 * This is an example of a simple windowed render loop
 */
public class Psim {

    static Graphics2D g2d = null;
    static String keys = "";
    static BufferedImage bi = null;
    static Graphics graphics = null;
    static BufferStrategy buffer = null;
    static int PC = 0x8000;
    static long time = 0;
    static char[] Vram = new char [0x8000];
    static char[] SSD = new char [0x80000];
    static int SSD_state = 0;
    private static final int BANK_ADDR = 0xF000;
    static int BANK = 0;
    static Color backgroundColour = new Color(0,0,0);
    static boolean refresh = false;
    static boolean fast = false;
    static int cycle_speed = 0;
    static short MREG0 = 0;
    static short MREG1 = 0;

    public static void usage() {
        System.out.println("Usage: ./psim -d -s -f -v -m -c [program.bin] [start execution");
        System.out.println("d Debug, s Slow, f Fast, v Minimised video, m Start in Monitor, c Cycle accurate");
        System.exit(1);
    }

    public static void main( String[] args ) {

        int  xsize = 1280;
        int  ysize = 960;

        boolean debug = false;
        boolean slow = false;
        boolean video = true;
        boolean extended = false;
        boolean start_in_monitor = false;

        int start_address = 0x0000;

        String executable = null;
        if (args.length > 0) {
            for (String arg: args){
                if (arg.equals("-m")) {
                    start_in_monitor = true;
                } else if (arg.equals("-d")) {
                    debug = true;
                } else if (arg.equals("-c")) {
                    cycle_speed = 320;
                } else if (arg.equals("-f")) {
                    fast = true;
                } else if (arg.equals("-s")) {
                    slow = true;
                } else if (arg.equals("-v")) {
                    video = false;
                } else if (arg.equals("-e")) {
                    extended = true;
                } else if (arg.equals("-h")) {
                    usage();
                } else if (arg.startsWith("-")) {
                    System.out.printf("Unknown option %s\n", arg);
                    usage();
                } else {
                    if (executable == null) {
                        executable = arg;
                        start_address = 0x8000;
                    } else {
                        try {
                            start_address = Integer.parseInt(arg, 16);
                            if (start_address < 0 || start_address > 0xffff) {
                                System.out.println("Starting hex address must be between 0000 and FFFF");
                                usage();
                            }
                        } catch (java.lang.NumberFormatException e) {
                            System.out.println("Expected hex address, must be between 0000 and FFFF");
                            usage();
                        }
                    }
                }
            }
        }

        if (executable == null)
            start_in_monitor = true;

        // Create game window...
        JFrame frame = new JFrame();
        frame.setIgnoreRepaint( true );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        // Create canvas for painting...
        Canvas canvas = new Canvas();
        canvas.setIgnoreRepaint( true );
        canvas.setSize( xsize, ysize );

        // Add canvas to game window...
        frame.add( canvas );
        frame.pack();
        frame.setVisible( true );
        if (!video) frame.setState(Frame.ICONIFIED);

        // Create BackBuffer...
        canvas.createBufferStrategy( 2 );
        buffer = canvas.getBufferStrategy();

        // Get graphics configuration...
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();

        // Create off-screen drawing surface
        bi = gc.createCompatibleImage( xsize, ysize );

        // Objects needed for rendering...
        Color background = Color.BLACK;
        g2d = bi.createGraphics();
        g2d.setColor( background );
        g2d.fillRect( 0, 0, xsize, ysize );

        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                keys = keys + e.getKeyChar();
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        frame.setFocusable(true);
        canvas.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                int d = e.getKeyChar();
                if (d == 19) { // ^S - Save screen
                    save_screen();
                } else if (d == 6) { // ^F - Fast mode toggle
                    fast = !fast;
                    cycle_speed = 0;
                } else if (d == 24) { // ^X - eXit & save SSD
                    write_bytes("ssd.bin", SSD);
                    System.exit(0);
                } else if (d == 14) { // ^N - Normal speed
                    fast = false;
                    cycle_speed = 0;
                } else if (d == 3) { // ^C - Toggle cycle accurate speed
                    if (cycle_speed != 320) {
                        cycle_speed = 320;
                    } else {
                        cycle_speed = 160;
                    }
                    fast = false;
                } else if (d == 27) { // Esc - Reset CPU
                    keys = "";
                    PC = 0;
                } else if (d == 12) { // ^L - Load text file
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
                    FileFilter hexFileFilter =new FileNameExtensionFilter("Hex files", "hex");
                    fileChooser.addChoosableFileFilter(hexFileFilter);
                    fileChooser.setFileFilter(hexFileFilter);
                    int result = fileChooser.showOpenDialog(canvas);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        try {
                            String newKeys = "";
                            File selectedFile = fileChooser.getSelectedFile();
                            FileReader fr = new FileReader(selectedFile);   //Creation of File Reader object
                            BufferedReader br = new BufferedReader(fr);  //Creation of BufferedReader object
                            int c = 0;
                            while ((c = br.read()) != -1)         //Read char by Char
                            {
                                char character = (char) c;          //converting integer to char
                                newKeys = newKeys + character;
                            }
                            keys = keys + newKeys;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    keys = keys + e.getKeyChar();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        canvas.setFocusable(true);

        // Initialize simulation
        int A = 0;
        int B = 0;
        int T = 0;
        int AH = 0;
        int AL = 0;
        int PIPE0 = 0;
        int PIPE1 = 0;
        int PIPE2 = 0;
        int PIPE3 = 0;
        int FR = 0;
        int K = 0;
        int ALU_OUT = 0;

        int IRSHIFT  = 0;
        int FRSHIFT = 8;
        int CSHIFT = 0;
        int VSHIFT = 1;
        int ZSHIFT = 2;
        int NSHIFT = 3;
        int DSHIFT = 4;

        int LOAD_K_SHIFT = 5;
        int NO_FETCH_SHIFT = 6;
        int LOAD_CONSTANT = (1<<LOAD_K_SHIFT);
        int NO_FETCH = (1<<NO_FETCH_SHIFT);

        // PIPE 2 - bit [31-16]
        // Data Bus Assert - bits [18-16]
        int ALU_MASK = 0x1f;

        int DA_SHIFT = 16;
        int DA_NONE = 0x00<<DA_SHIFT;
        int DA_MEM = 0x01<<DA_SHIFT;
        int DA_ALU = 0x02<<DA_SHIFT;
        int DA_CONSTANT = 0x03<<DA_SHIFT;
        int DA_IO = 0x04<<DA_SHIFT;
        int DA_IOFLAGS = 0x05<<DA_SHIFT;
        int DA_MASK = 0x7<<DA_SHIFT;

        // Data Bus Read - bits [22-19]
        int DR_SHIFT = 19;
        int DR_NONE = 0x00<<DR_SHIFT;
        int DR_MEM = 0x01<<DR_SHIFT;
        int DR_A = 0x02<<DR_SHIFT;
        int DR_B = 0x03<<DR_SHIFT;
        int DR_T = 0x04<<DR_SHIFT;
        int DR_PC = 0x05<<DR_SHIFT;
        int DR_MARH = 0x06<<DR_SHIFT;
        int DR_MARL = 0x07<<DR_SHIFT;
        int DR_IO = 0x08<<DR_SHIFT;
        int DR_MASK = 0x0F<<DR_SHIFT;

        int ADDRESS_SHIFT = 23;
        int BUS_REQ_SHIFT = 24;
        int ADDRESS_ASSERT = (1<<ADDRESS_SHIFT);
        int BUS_REQUEST = (1<<BUS_REQ_SHIFT);

        String [] ALUop = {
            "0",
            "A",
            "B",
            "-A",
            "-B",
            "A+1",
            "B+1",
            "A-1",
            "B-1",
            "A+B",
            "A+B+1",
            "A-B",
            "A-Bspecial",
            "B-A",
            "A-B-1",
            "B-A-1",
            "A*BHI",
            "A*BLO",
            "A/B",
            "A%B",
            "A<<B",
            "A>>BL",
            "A>>BA",
            "AROLB",
            "ARORB",
            "A&B",
            "A|B",
            "A^B",
            "!A",
            "!B",
            "ADIVB",
            "AREMB"
        };

        // Load in binary files for ALU, Decode, Rom, Ram and Vram
        char[] ALURom = new char [0x400000];
        char[] ctrl1a = new char[1<<12];
        char[] ctrl1b = new char[1<<12];
        char[] ctrl2a = new char[1<<12];
        char[] ctrl2b = new char[1<<12];
        char[] Rom = new char [0x8000];
        char[] Ram = new char [0x8000];

        // Randomise Ram
        for(int i=0; i<Ram.length; i++) {
            Ram[i] = (char)(256*Math.random());
        }

        for(int i=0; i<Vram.length; i++) {
            Vram[i] = (char)(256*Math.random());
//            Vram[i] = (char)(i%0xff);
        }


        try {
            read_bytes("alu.bin", ALURom, 0);
            read_bytes("ctrl1a.bin", ctrl1a, 0);
            read_bytes("ctrl1b.bin", ctrl1b, 0);
            read_bytes("ctrl2a.bin", ctrl2a, 0);
            read_bytes("ctrl2b.bin", ctrl2b, 0);
            read_bytes("instr.bin", Rom, 0);
            read_bytes("ssd.bin", SSD, 0);
            if (executable != null) {
                if (start_address >= 0x8000) {
                    read_bytes(executable, Ram, start_address - 0x8000);
                } else {
                    read_bytes(executable, Ram, 0);
                }
                PC = start_address;
            } else {
                PC = 0x0000;
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        if (start_in_monitor)
            PC = 0x0000;

        long last = System.nanoTime();
        long now = last;

        boolean carry = false;
        boolean overflow = false;
        boolean zero = false;
        boolean negative = false;
        int flags = 0;

        while( true ) {
            try {
                // Cycle accurate timing
                if (cycle_speed > 0) {
                    do {
                        now = System.nanoTime();
                    } while (now - last < cycle_speed); // 320 ~ 3.15MHz
                }
                last = now;

                // Move last instruction up pipeline
                PIPE3 = PIPE2;
                PIPE2 = PIPE1;
                PIPE1 = PIPE0;

                // Work out the decode PIPELINE ROM index
                int decodeidx1 = (FR << FRSHIFT) | (PIPE1 << IRSHIFT);
                int decodeidx2 = (FR << FRSHIFT) | (PIPE2 << IRSHIFT);

                // Get the microinstruction
                int ctrl1 = ((((char) ctrl1a[decodeidx1]) << 8) | ((char) ctrl1b[decodeidx1]));
                int ctrl2 = ((((char) ctrl2a[decodeidx2]) << 8) | ((char) ctrl2b[decodeidx2]));
                int ctrl = ctrl2 << 16 | ctrl1;

                // Check Fetch condition
                boolean fetch_stage_1 = false;
                boolean fetch_stage_2 = false;
                if ((ctrl & NO_FETCH) == 0) {
                    fetch_stage_1 = true;
                }
                if ((ctrl & BUS_REQUEST) == 0) {
                    fetch_stage_2 = true;
                }


                if (debug) {
                    System.out.printf("PC:%04x P0:%02x P1:%02x P2:%02x FR:%02x CTRL:%08x\n",
                            PC, PIPE1, PIPE2, PIPE3, FR, ctrl);
                }
                if (slow) {
                    // Wait one second
                    wait(100);
                }

                //-------- Stage 2 Pipeline -------------------
                int data_assert = ctrl & DA_MASK;
                int data_read = ctrl & DR_MASK;
                int address_assert = ctrl & ADDRESS_ASSERT;
                int bus_req = ctrl & BUS_REQUEST;

                if (fetch_stage_2)
                    PC = (PC + 1) & 0xffff;

                // Determine if Memory Address is PC or MAR
                int mem_address = (address_assert == ADDRESS_ASSERT) ? ((AH << 8) | AL) : PC;

                // Assert to Databus
                int data_bus = 0;
                if (data_assert == DA_MEM) {
//                    data_bus = PIPE0;
                    if (mem_address >= 0x8000) {
                        data_bus = Ram[mem_address - 0x8000];
                    } else {
                        data_bus = Rom[mem_address];
                    }
                } else if (data_assert == DA_ALU) {
                    data_bus = ALU_OUT;
                } else if (data_assert == DA_CONSTANT) {
                    data_bus = K;
                } else if (data_assert == DA_IO) {
                    if (keys.length() > 0) {
                        data_bus = keys.charAt(0);
                        if (data_bus != 0) {
                          keys = keys.substring(1);
                        }
                    } else {
                        data_bus = 0;
                    }
                }

                //Load from the data bus
                if (data_read == DR_A) {
                    A = data_bus;
                    if (debug)
                        System.out.printf("->A %02x\n", A);
                }
                if (data_read == DR_B) {
                    B = data_bus;
                    if (debug)
                        System.out.printf("->B %02x\n", B);
                }
                if (data_read == DR_T) {
                    T = data_bus;
                    if (debug)
                        System.out.printf("->T %02x\n", T);
                }
                if (data_read == DR_MEM) {
                    if (mem_address >= 0x8000) {
                        Ram[mem_address-0x8000] = (char) data_bus;
                    } else {
                        // TODO: Bank Register (SSD etc)
                        Vram[mem_address] = (char) data_bus;
                    }
                    if (debug) System.out.printf("%04x ->MEM %02x\n", mem_address, data_bus);
                }
//                if (data_read == DR_MEM) {
//                    // Memory mapped I/O - BANK
//                    if (address == BANK_ADDR) {
//                        BANK = databus;
//                    }
//                    // RAM
//                    if (address >= 0x8000) {
//                        Ram[address - 0x8000] = (char) databus;
//                        if (debug)
//                            System.out.printf("->RAM %04x %02x\n", address - 0x8000, (byte) Ram[address - 0x8000]);
//                    } else {
//                        // Use BANK register to determine VGA or SSD memory
//                        if (BANK == 0) {
//                            plot(address, databus, Vram[address]);
//                            Vram[address] = (char) databus;
//                            if (debug)
//                                System.out.printf("->VRAM %04x %02x\n", address, (byte) Vram[address]);
//                        } else if (BANK >= 0xF0) {
//                            set_ssd(address, databus, debug);
//                            if (debug)
//                                System.out.printf("->SSD [%02x] %04x %02x\n", BANK, address, (byte) Vram[address]);
//                        } else if (BANK == 0x10) {
//                            int a = address % 4;
//                            if (a == 0) {
//                                MREG0 = (short) ((MREG0 & 0x00ff) | ((short) databus << 8));
//                            } else if (a == 1) {
//                                MREG0 = (short) ((MREG0 & 0xff00) | ((short) databus << 0));
//                            } else if (a == 2) {
//                                MREG1 = (short) ((MREG1 & 0x00ff) | ((short) databus << 8));
//                            } else if (a == 3) {
//                                MREG1 = (short) ((MREG1 & 0xff00) | ((short) databus << 0));
//                            }
//                        }
//                    }
//                }
                if (data_read == DR_MARH) {
                    AH = data_bus;
                    if (debug)
                        System.out.printf("->AH %02x\n", AH);
                }
                if (data_read == DR_MARL) {
                    AL = data_bus;
                    if (debug)
                        System.out.printf("->AL %02x\n", AL);
                }
                if (data_read == DR_IO) {
                    System.out.printf("%c", data_bus); // Flush the output
                    if (debug)
                        System.out.printf("->IO %02x\n", data_bus);
                }
                if (data_read == DR_PC) {
                    PC = (T<<8) | data_bus;
                    if (debug)
                        System.out.printf("->PC %02x\n", data_bus);
                }
                if (debug) {
                    System.out.printf("Address %04x data_read %02x data_ass %02x data_bus %02x \n", mem_address, data_read, data_assert, data_bus);
                }


                //-------- Stage 1 Pipeline -------------------
                int load_constant = ctrl & LOAD_CONSTANT;
                int no_fetch = ctrl & NO_FETCH;

                if (load_constant == LOAD_CONSTANT) {
                    if (mem_address >= 0x8000) {
                        K = Ram[mem_address - 0x8000];
                    } else {
                        K = Rom[mem_address];
                    }
                }

                // Do ALU computation
                int alu_op = ctrl & ALU_MASK;
                int alu_addr = ((alu_op << 16) | (A << 8) | B) * 2;
                int aluresult = ((ALURom[alu_addr + 1] & 0xff) << 8) | (ALURom[alu_addr] & 0xff);
                if (debug) {
                    System.out.printf("A:%02x B:%02x OP:%s RES:%04x \n", A, B, ALUop[alu_op], aluresult);
                }
                ALU_OUT = aluresult & 0xff;

                // Store value of flags in FR
                if (data_assert == DA_ALU) {
                    FR = flags;
                    // Extract the flags from the result, and remove from the result
                    carry = ((FR >> CSHIFT) & 1) == 1;
                    overflow = ((FR >> VSHIFT) & 1) == 1;
                    zero = ((FR >> ZSHIFT) & 1) == 1;
                    negative = ((FR >> NSHIFT) & 1) == 1;
                    if (debug) {
                        System.out.printf("FL %04x C:%b O:%b Z:%b N:%b\n", aluresult, carry, overflow, zero, negative);
                    }
                }
                flags = ALURom[alu_addr + 1] & 0x01f;

                if (debug)
                    System.out.println();
                
                // Do graphics
                // Blit image and flip every so often
                if (System.currentTimeMillis() - time > 20) {
                    refreshScreen();
                    graphics = buffer.getDrawGraphics();
                    graphics.drawImage(bi, 0, 0, null);
                    if (!buffer.contentsLost())
                        buffer.show();
                    time = System.currentTimeMillis();
                }

                // Fetch next instruction (Unless BUSREQ and MEMREQ)
                if (fetch_stage_1 && fetch_stage_2) {
                    if (PC >= 0x8000)
                        PIPE0 = (char) Ram[PC - 0x8000];
                    else
                        PIPE0 = (char) Rom[PC];
                } else if (fetch_stage_1 || fetch_stage_2) {
                    PIPE0 = 0;
                } else {
                    PIPE0 = PIPE1;
                }
                // Let the OS have a little time...
                if (!fast && cycle_speed == 0)
                    Thread.yield();

                if (PIPE1 == 0xff) {
                    System.exit(0);
                }
            } finally {
                // release resources
                if( graphics != null )
                    graphics.dispose();
                if( g2d != null )
                    g2d.dispose();
            }
        }
    }

    // SSD stuff

    private static final int STEP_RESET = 0;
    private static final int STEP_1 = 1;
    private static final int STEP_2 = 2;
    private static final int STEP_3 = 3;
    private static final int STEP_4 = 4;
    private static final int STEP_5 = 5;
    private static final int BYTE_WRITE = 6;

    // Set SSD data according to state machine
    static private void set_ssd(int address, int data, boolean debug) {
        if (debug)
            System.out.printf("Step [%d] addr [%04x], data [%02x]\n", SSD_state, address, data);
        if (SSD_state == STEP_RESET && address == 0x5555 && data == 0xAA)
            SSD_state = STEP_1;
        else if (SSD_state == STEP_1 && address == 0x2AAA && data == 0x55)
            SSD_state = STEP_2;
        else if (SSD_state == STEP_2 && address == 0x5555 && data == 0xA0)
            SSD_state = BYTE_WRITE;
        else if (SSD_state == BYTE_WRITE && BANK >= 0xF0) {
            int addr = (address & 0x7FFF) + 0x8000 * (BANK & 0x0F);
            if (SSD[addr] == 0xFF) {
                SSD[addr] = (char) data;
            }
            SSD_state = STEP_RESET;
        }
        else if (SSD_state == STEP_2 && address == 0x5555 && data == 0x80)
            SSD_state = STEP_3;
        else if (SSD_state == STEP_3 && address == 0x5555 && data == 0xAA)
            SSD_state = STEP_4;
        else if (SSD_state == STEP_4 && address == 0x2AAA && data == 0x55)
            SSD_state = STEP_5;
        else if (SSD_state == STEP_5 && address == 0x5555 && data == 0x10) {
            // Chip erase
            for (int i=0; i<0x80000; i++) {
                SSD[i] = 0xFF;
            }
            SSD_state = STEP_RESET;
        }
        else if (SSD_state == STEP_5 && data == 0x30) {
            // Sector erase
            int addr = (address & 0x7FFF) + 0x8000 * (BANK & 0x0F);
            int sec = addr & 0xFF000;
            for (int i=0; i<0x1000; i++) {
                SSD[sec+i] = 0xFF;
            }
            SSD_state = STEP_RESET;
        }
        else
            SSD_state = STEP_RESET;
    }

    static private void refreshScreen(){
        for (int y=0; y<120; y++) {
            for (int x=0; x<160; x++) {
                int addr = y<< 8 | x;
                plot(addr, Vram[addr], 0);
            }
        }
        refresh = false;
    }

    static private void plot(int addr, int colour, int current) {
        final int size = 8;
        final int half = size/2;
        int x = addr & 0xFF;
        int y = addr >> 8;

        if (colour < 128) {
            int r = ((colour >> 4) & 3)*85;
            int g = ((colour >> 2) & 3)*85;
            int b = ((colour & 3) * 85);

            g2d = bi.createGraphics();
            Color col = new Color(r, g, b);
            g2d.setColor(col);
            g2d.fillRect(x * size, y * size, size, size);

            if (colour >= 64) {
                backgroundColour = col;
                refresh = true;
            }
        } else {
            int r = ((colour >> 6)&1)*255;
            int g = ((colour >> 5)&1)*255;
            int b = ((colour >> 4)&1)*255;
            // Use Orange as colour 0 (not black)
            if (r+g+b == 0) {
                r = 3<<6;
                g = 4<<4;
                b = 0;
            }
            g2d = bi.createGraphics();
            Color back = backgroundColour;
            Color fore = new Color(r, g, b);
            if ((colour&1) > 0) {
                g2d.setColor(fore);
                g2d.fillRect(x * size + half, y * size, half, half);
            } else {
                g2d.setColor(back);
                g2d.fillRect(x * size + half, y * size, half, half);
            }
            if ((colour&2) > 0) {
                g2d.setColor(fore);
                g2d.fillRect(x * size, y * size, half, half);
            } else {
                g2d.setColor(back);
                g2d.fillRect(x * size, y * size, half, half);
            }
            if ((colour&4) > 0) {
                g2d.setColor(fore);
                g2d.fillRect(x * size + half, y * size + half, half, half);
            } else {
                g2d.setColor(back);
                g2d.fillRect(x * size + half, y * size + half, half, half);
            }
            if ((colour&8) > 0) {
                g2d.setColor(fore);
                g2d.fillRect(x * size, y * size + half, half, half);
            } else {
                g2d.setColor(back);
                g2d.fillRect(x * size, y * size + half, half, half);
            }
        }
    }

    static private void read_bytes(String filename, char data[], int addr) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            while (true) {
                data[addr++] = (char) in.readUnsignedByte();
            }
        } catch (EOFException e) {
            System.out.printf("File %s loaded\n", filename);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static private void write_bytes(String filename, char data[]) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            int addr = 0;
            while (addr < data.length) {
                out.write(data[addr++]);
            }
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static private String int2hex(int i) {
        String hex = Integer.toHexString(i);
        if (hex.length()==1) hex = "0"+hex;
        return hex;
    }

    static private void save_screen() {
        String filename = "screen.bin";
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            for (int address = 0; address < 0x8000; address++) {
                out.writeByte(Vram[address]);
            }
            out.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        filename = "screen.hex";
        try {
            FileOutputStream outputStream = new FileOutputStream(filename);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

            for (int y=0; y<120; y++) {
                bufferedWriter.write("C" + int2hex(y) + "00\n");
                for (int x=0; x<160; x++) {
                    int addr = y*256 + x;
                    int b = Vram[addr];
                    String hex = int2hex(b);
                    bufferedWriter.write(hex+" ");
                }
                bufferedWriter.write("z\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void wait(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
}
