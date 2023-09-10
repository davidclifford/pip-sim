import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
public class GenControl {

    /************************************
     * a = high byte, b = low byte
     *
     * PIPE1
     *
     * ALU_OP        (5-bits)
     * LOAD_CONSTANT (1-bit)
     * MEM_REQ       (1-bit)
     *               7-bits
     *
     * PIPE2
     *
     * DATA_ASSERT      (3-bits)
     * DATA_READ        (4-bits)
     * ADDRESS_ASSERT   (1-bit)
     * BUS_REQ          (1-bit)
     *                  9-bits
     *
     ************************************/

    // PIPE 1 - bits [15-0] 16 bits
    // ALU OP - bits [4-0] 5 bits 32 ops
    public final static int ALU_ZERO = 0x00;
    public final static int ALU_A = 0x01;
    public final static int ALU_B = 0x02;
    public final static int ALU_NEGA = 0x03;
    public final static int ALU_NEGB = 0x04;
    public final static int ALU_INCA = 0x05;
    public final static int ALU_INCB = 0x06;
    public final static int ALU_DECA = 0x07;
    public final static int ALU_DECB = 0x08;
    public final static int ALU_APLUSB = 0x09;
    public final static int ALU_APLUSBPLUS1 = 0x0a;
    public final static int ALU_AMINUSB = 0x0b;
    public final static int ALU_AMINUSB2 = 0x0c;
    public final static int ALU_BMINUSA = 0x0d;
    public final static int ALU_AMINUSBMINUS1 = 0x0e;
    public final static int ALU_BMINUSAMINUS1 = 0x0f;
    public final static int ALU_AMULTBHI = 0x10;
    public final static int ALU_AMULTBLO = 0x11;
    public final static int ALU_ADIVB = 0x12;
    public final static int ALU_AMODB = 0x13;
    public final static int ALU_ASHLB = 0x14;
    public final static int ALU_ASRLB = 0x15;
    public final static int ALU_ASRAB = 0x16;
    public final static int ALU_AROLB = 0x17;
    public final static int ALU_ARORB = 0x18;
    public final static int ALU_AANDB = 0x19;
    public final static int ALU_AORB = 0x1a;
    public final static int ALU_AXORB = 0x1b;
    public final static int ALU_NOTA = 0x1c;
    public final static int ALU_NOTB = 0x1d;
    public final static int ALU_ADIV10B = 0x1e;
    public final static int ALU_AMOD10B = 0x1f;
    public static final int ALU_MASK = 0x1f;

    public final static int LOAD_CONSTANT = (1<<5);
    public final static int NO_FETCH = (1<<6);

    // PIPE 2 - bit [31-16]
    // Data Bus Assert - bits [18-16] 3 bits
    public final static int DA_NONE = 0x00<<16;
    public final static int DA_MEM = 0x01<<16;
    public final static int DA_ALU = 0x02<<16;
    public final static int DA_CONSTANT = 0x03<<16;
    public final static int DA_IO = 0x04<<16;
    public final static int DA_IOFLAGS = 0x05<<16;
    public final static int DA_MASK = 0x7<<16;

    // Data Bus Read - bits [22-19] 4 bits
    public final static int DR_NONE = 0x00<<19;
    public final static int DR_MEM = 0x01<<19;
    public final static int DR_A = 0x02<<19;
    public final static int DR_B = 0x03<<19;
    public final static int DR_T = 0x04<<19;
    public final static int DR_PC = 0x05<<19;
    public final static int DR_MARH = 0x06<<19;
    public final static int DR_MARL = 0x07<<19;
    public final static int DR_IO = 0x08<<19;
    public static final int DR_MASK = 0x0F<<19;

    public static final int ADDRESS_ASSERT = (1<<23);
    public static final int BUS_REQUEST = (1<<24);

    private static char[] ctrl1a = new char[1<<12];
    private static char[] ctrl1b = new char[1<<12];
    private static char[] ctrl2a = new char[1<<12];
    private static char[] ctrl2b = new char[1<<12];

    private static int do_instruction(int opcode, int flags) {
        boolean carry = (flags & 0x01) != 0;
        boolean zero = ((flags >> 1) & 0x01) != 0;
        boolean neg = ((flags >> 2) & 0x01) != 0;
        boolean over = ((flags >> 3) & 0x01) != 0;

        int control_word = 0;

        //NOP
        // DO NOTHING

        // CLR A (A=0)
        if (opcode == 0x01) {
            control_word |= ALU_ZERO | DA_ALU | DR_A;
        }
        // MOV A,B
        else if (opcode == 0x02) {
            control_word |= ALU_B | DA_ALU | DR_A;
        }
        // INC A
        else if (opcode == 0x03) {
            control_word |= ALU_INCA | DA_ALU | DR_A;
        }
        // DEC A
        else if (opcode == 0x04) {
            control_word |= ALU_DECA | DA_ALU | DR_A;
        }
        // ADD A,B
        else if (opcode == 0x05) {
            control_word |= ALU_APLUSB | DA_ALU | DR_A;
        }
        // ADC A,B
        else if (opcode == 0x06) {
            if (carry) control_word |= ALU_APLUSBPLUS1 | DA_ALU | DR_A;
            else control_word |= ALU_APLUSB | DA_ALU | DR_A;
        }
        // SUB A,B
        else if (opcode == 0x07) {
            control_word |= ALU_AMINUSB | DA_ALU | DR_A;
        }
        // SBB A,B
        else if (opcode == 0x08) {
            if (carry) control_word |= ALU_AMINUSBMINUS1 | DA_ALU | DR_A;
            else control_word |= ALU_AMINUSBMINUS1 | DA_ALU | DR_A;
        }

        return control_word;
    }

    public static String toBinary(int num) {
        String out = "";
        for (int i=0; i<4; i++) {
            String bits = "00000000" + Integer.toBinaryString(num & 0xff);
            out = bits.substring(bits.length()-8) + " " + out;
            num >>= 8;
        }
        return out;
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

    public static void main(String[] args) {
        // TODO: Read ROMs first and compare to indicate which ROMs changed
        System.out.println("Creating Control Signals for Pip pipelined CPU");
        for (int flags = 0; flags < 2; flags++) {
            for (int opcode = 0; opcode < 0x100; opcode++) {
                int control_word = do_instruction(opcode, flags);
                System.out.printf("%02x %08x   %s\n", opcode, control_word, toBinary(control_word));
                int addr = opcode | flags << 8;
                ctrl1b[addr] = (char) ((control_word) & 0xff);
                ctrl1a[addr] = (char) ((control_word >> 8) & 0xff);
                ctrl2b[addr] = (char) ((control_word >> 16) & 0xff);
                ctrl2a[addr] = (char) ((control_word >> 24) & 0xff);
            }
        }
        // Write ROMS
        write_bytes("ctrl1a.bin", ctrl1a);
        write_bytes("ctrl1b.bin", ctrl1b);
        write_bytes("ctrl2a.bin", ctrl2a);
        write_bytes("ctrl2b.bin", ctrl2b);
    }
}