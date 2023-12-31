import java.io.*;

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

    private static String[] opcodes = new String[256];

    private static int do_instruction(int opcode, int flags, BufferedWriter bw) throws IOException
    {
        boolean carry = (flags & 0x01) != 0;
        boolean over = (flags & 0x02) != 0;
        boolean zero = (flags & 0x04) != 0;
        boolean neg = (flags & 0x08) != 0;

        int control_word = 0;
        String mnemonic = null;
        int bytes = 1;

        //NOP
        // DO NOTHING
        if (opcode == 0x00) {
            mnemonic = "NOP";
        }

        // CLR A (A=0)
        else if (opcode == 0x01) {
            control_word |= ALU_ZERO | DA_ALU | DR_A;
            mnemonic = "CLR_A";
        }
        // MOV A,B
        else if (opcode == 0x02) {
            control_word |= ALU_B | DA_ALU | DR_A;
            mnemonic = "MOV_A,B";
        }
        // NEG A
        else if (opcode == 0x03) {
            control_word |= ALU_NEGA | DA_ALU | DR_A;
            mnemonic = "NEG_A";
        }
        // INC A
        else if (opcode == 0x04) {
            control_word |= ALU_INCA | DA_ALU | DR_A;
            mnemonic = "INC_A";
        }
        // DEC A
        else if (opcode == 0x05) {
            control_word |= ALU_DECA | DA_ALU | DR_A;
            mnemonic = "DEC_A";
        }
        // ADD A,B
        else if (opcode == 0x06) {
            control_word |= ALU_APLUSB | DA_ALU | DR_A;
            mnemonic = "ADD_A,B";
        }
        // ADC A,B
        else if (opcode == 0x07) {
            if (carry) control_word |= ALU_APLUSBPLUS1 | DA_ALU | DR_A;
            else control_word |= ALU_APLUSB | DA_ALU | DR_A;
            mnemonic = "ADC_A,B";
        }
        // SUB A,B
        else if (opcode == 0x08) {
            control_word |= ALU_AMINUSB | DA_ALU | DR_A;
            mnemonic = "SUB_A,B";
        }
        // SBB A,B
        else if (opcode == 0x09) {
            if (carry) control_word |= ALU_AMINUSBMINUS1 | DA_ALU | DR_A;
            else control_word |= ALU_AMINUSB | DA_ALU | DR_A;
            mnemonic = "SBB_A,B";
        }
        // MLL A,B
        else if (opcode == 0x0a) {
            control_word |= ALU_AMULTBLO | DA_ALU | DR_A;
            mnemonic = "MLL_A,B";
        }
        // MLH A,B
        else if (opcode == 0x0b) {
            control_word |= ALU_AMULTBHI | DA_ALU | DR_A;
            mnemonic = "MLH_A,B";
        }
        // DIV A,B
        else if (opcode == 0x0c) {
            control_word |= ALU_ADIVB | DA_ALU | DR_A;
            mnemonic = "DIV_A,B";
        }
        // MOD A,B
        else if (opcode == 0x0d) {
            control_word |= ALU_AMODB | DA_ALU | DR_A;
            mnemonic = "MOD_A,B";
        }
        // AND A,B
        else if (opcode == 0x0e) {
            control_word |= ALU_AANDB | DA_ALU | DR_A;
            mnemonic = "AND_A,B";
        }
        // OR A,B
        else if (opcode == 0x0f) {
            control_word |= ALU_AORB | DA_ALU | DR_A;
            mnemonic = "OR_A,B";
        }
        // XOR A,B
        else if (opcode == 0x10) {
            control_word |= ALU_AXORB | DA_ALU | DR_A;
            mnemonic = "XOR_A,B";
        }
        // NOT A
        else if (opcode == 0x011) {
            control_word |= ALU_NOTA | DA_ALU | DR_A;
            mnemonic = "NOT_A";
        }
        // DIV10
        else if (opcode == 0x12) {
            control_word |= ALU_ADIV10B | DA_ALU | DR_A;
            mnemonic = "DIV10_A,B";
        }
        // MOD10
        else if (opcode == 0x13) {
            control_word |= ALU_AMOD10B | DA_ALU | DR_A;
            mnemonic = "MOD10_A,B";
        }


        // CLR B (B=0)
        else if (opcode == 0x21) {
            control_word |= ALU_ZERO | DA_ALU | DR_B;
            mnemonic = "CLR_B";
        }
        // MOV B, A
        else if (opcode == 0x22) {
            control_word |= ALU_A | DA_ALU | DR_B;
            mnemonic = "MOV_B,A";
        }
        // NEG B
        else if (opcode == 0x23) {
            control_word |= ALU_NEGB | DA_ALU | DR_B;
            mnemonic = "NEG_B";
        }
        // INC B
        else if (opcode == 0x24) {
            control_word |= ALU_INCB | DA_ALU | DR_B;
            mnemonic = "INC_B";
        }
        // DEC B
        else if (opcode == 0x25) {
            control_word |= ALU_DECB | DA_ALU | DR_B;
            mnemonic = "DEC_B";
        }
        // ADD B,A
        else if (opcode == 0x26) {
            control_word |= ALU_APLUSB | DA_ALU | DR_B;
            mnemonic = "ADD_B,A";
        }
        // ADC B,A
        else if (opcode == 0x27) {
            if (carry) control_word |= ALU_APLUSBPLUS1 | DA_ALU | DR_B;
            else control_word |= ALU_APLUSB | DA_ALU | DR_B;
            mnemonic = "ADC_B,A";
        }
        // SUB B,A
        else if (opcode == 0x28) {
            control_word |= ALU_BMINUSA | DA_ALU | DR_B;
            mnemonic = "SUB_B,A";
        }
        // SBB B,A
        else if (opcode == 0x29) {
            if (carry) control_word |= ALU_BMINUSAMINUS1 | DA_ALU | DR_B;
            else control_word |= ALU_BMINUSA | DA_ALU | DR_B;
            mnemonic = "SBB_B,A";
        }
        // MLL B,A
        else if (opcode == 0x2a) {
            control_word |= ALU_AMULTBLO | DA_ALU | DR_B;
            mnemonic = "MLL_B,A";
        }
        // MLH B,A
        else if (opcode == 0x2b) {
            control_word |= ALU_AMULTBHI | DA_ALU | DR_B;
            mnemonic = "MLH_B,A";
        }
        // DVB A,B
        else if (opcode == 0x2c) {
            control_word |= ALU_ADIVB | DA_ALU | DR_B;
            mnemonic = "DVB_A,B";
        }
        // MDB A,B
        else if (opcode == 0x2d) {
            control_word |= ALU_AMODB | DA_ALU | DR_B;
            mnemonic = "MDB_A,B";
        }
        // AND B,A
        else if (opcode == 0x2e) {
            control_word |= ALU_AANDB | DA_ALU | DR_B;
            mnemonic = "AND_B,A";
        }
        // OR B,A
        else if (opcode == 0x2f) {
            control_word |= ALU_AORB | DA_ALU | DR_B;
            mnemonic = "OR_B,A";
        }
        // XOR B,A
        else if (opcode == 0x30) {
            control_word |= ALU_AXORB | DA_ALU | DR_B;
            mnemonic = "XOR_B,A";
        }
        // NOT B
        else if (opcode == 0x031) {
            control_word |= ALU_NOTB | DA_ALU | DR_B;
            mnemonic = "NOT_B";
        }
        // DVB10
        else if (opcode == 0x32) {
            control_word |= ALU_ADIV10B | DA_ALU | DR_B;
            mnemonic = "DVB10_A,B";
        }
        // MDB10
        else if (opcode == 0x33) {
            control_word |= ALU_AMOD10B | DA_ALU | DR_B;
            mnemonic = "MDB10_A,B";
        }
        // CMP A,B
        else if (opcode == 0x34) {
            control_word |= ALU_AMINUSB | DA_ALU ;
            mnemonic = "CMP_A,B";
        }
        // CMP B,A
        else if (opcode == 0x35) {
            control_word |= ALU_BMINUSA | DA_ALU ;
            mnemonic = "CMP_B,A";
        }
        // CMP A
        else if (opcode == 0x36) {
            control_word |= ALU_A | DA_ALU ;
            mnemonic = "CMP_A";
        }
        // CMP B
        else if (opcode == 0x37) {
            control_word |= ALU_B | DA_ALU ;
            mnemonic = "CMP_B";
        }
        // MOV L,A
        else if (opcode == 0x38) {
            control_word |= ALU_A | DA_ALU | DR_MARL;
            mnemonic = "MOV_L,A";
        }
        // MOV L,B
        else if (opcode == 0x39) {
            control_word |= ALU_B | DA_ALU | DR_MARL;
            mnemonic = "MOV_L,B";
        }
        // MOV H,A
        else if (opcode == 0x3a) {
            control_word |= ALU_A | DA_ALU | DR_MARH;
            mnemonic = "MOV_H,A";
        }
        // MOV H,B
        else if (opcode == 0x3b) {
            control_word |= ALU_B | DA_ALU | DR_MARH;
            mnemonic = "MOV_H,B";
        }

        // STO
        else if (opcode == 0x40) {
            control_word |= DA_CONSTANT | DR_MEM | ADDRESS_ASSERT | BUS_REQUEST | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "STO";
            bytes = 2;
        }

        // STO 0 (MEM=0)
        else if (opcode == 0x41) {
            control_word |= ALU_ZERO | DA_ALU | DR_MEM | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "STO 0";
        }
        // STO A
        else if (opcode == 0x42) {
            control_word |= ALU_A | DA_ALU | DR_MEM | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "STO_A";
        }
        // STO B
        else if (opcode == 0x43) {
            control_word |= ALU_B | DA_ALU | DR_MEM | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "STO_B";
        }
        // LDA
        else if (opcode == 0x44) {
            control_word |= DA_MEM | DR_A | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "LDA";
        }
        // LDB
        else if (opcode == 0x45) {
            control_word |= DA_MEM | DR_B | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "LDB";
        }
        // MOV A immediate
        else if (opcode == 0x46) {
            control_word |= DA_CONSTANT | DR_A | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "MOV_A";
            bytes = 2;
        }
        // MOV B immediate
        else if (opcode == 0x47) {
            control_word |= DA_CONSTANT | DR_B | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "MOV_B";
            bytes = 2;
        }
        // MOV H immediate
        else if (opcode == 0x48) {
            control_word |= DA_CONSTANT | DR_MARH | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "MOV_H";
            bytes = 2;
        }
        // MOV L immediate
        else if (opcode == 0x49) {
            control_word |= DA_CONSTANT | DR_MARL | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "MOV_L";
            bytes = 2;
        }
        // MOV T immediate (for jumps)
        else if (opcode == 0x4a) {
            control_word |= DA_CONSTANT | DR_T | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "MOV_T";
            bytes = 2;
        }
        // OUT constant
        else if (opcode == 0x4b) {
            control_word |= DA_CONSTANT | DR_IO | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "OUT";
            bytes = 2;
        }
        // OUT A
        else if (opcode == 0x4c) {
            control_word |= ALU_A | DA_ALU | DR_IO;
            mnemonic = "OUT_A";
        }
        // OUT B
        else if (opcode == 0x4d) {
            control_word |= ALU_B | DA_ALU | DR_IO;
            mnemonic = "OUT_B";
        }
        // IN A
        else if (opcode == 0x4e) {
            control_word |= DA_IO | DR_A;
            mnemonic = "IN_A";
        }
        // IN B
        else if (opcode == 0x4f) {
            control_word |= DA_IO | DR_B;
            mnemonic = "IN_B";
        }


        // JUMPS
        // Unconditional JMP
        else if (opcode == 0x50) {
            control_word |= DA_CONSTANT | DR_PC | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "JMP";
            bytes = 2;
        }
        // JGE
        else if (opcode == 0x51) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (!carry)
                control_word |= DA_CONSTANT | DR_PC ;
            mnemonic = "JGE";
            bytes = 2;
        }
        // JLT
        else if (opcode == 0x52) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (carry)
                control_word |= DA_CONSTANT | DR_PC;
            mnemonic = "JLT";
            bytes = 2;
        }
        // JEQ
        else if (opcode == 0x53) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (zero)
                control_word |= DA_CONSTANT | DR_PC;
            mnemonic = "JEQ";
            bytes = 2;
        }
        // JNE
        else if (opcode == 0x54) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (!zero)
                control_word |= DA_CONSTANT | DR_PC;
            mnemonic = "JNE";
            bytes = 2;
        }
        // JPV
        else if (opcode == 0x55) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (over)
                control_word |= DA_CONSTANT | DR_PC;
            mnemonic = "JPV";
            bytes = 2;
        }
        // JPN
        else if (opcode == 0x56) {
            control_word = NO_FETCH | LOAD_CONSTANT;
            if (neg)
                control_word |= DA_CONSTANT | DR_PC;
            mnemonic = "JPN";
            bytes = 2;
        }

        // LDT
        else if (opcode == 0x60) {
            control_word |= DA_MEM | DR_T | ADDRESS_ASSERT | BUS_REQUEST;
            mnemonic = "LDT";
        }
        // LDP
        else if (opcode == 0x61) {
            control_word |= DA_MEM | DR_PC | ADDRESS_ASSERT ;
            mnemonic = "LDP";
        }
        // JSR (macro in the assembler)
        else if (opcode == 0x62) {
            //control_word |= DA_CONSTANT | DR_PC | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "JSR";
            bytes = 14;
        }
        // RTS (macro in the assembler)
        else if (opcode == 0x63) {
            //control_word |= DA_CONSTANT | DR_PC | LOAD_CONSTANT | NO_FETCH;
            mnemonic = "RTS";
            bytes = 8;
        }

        //HLT PSEUDO INSTRUCTION actually a NOP
        else if (opcode == 0xff) {
            mnemonic = "HLT";
        }

        if (mnemonic != null && flags == 0x00) {
            bw.write(String.format("%02x %d %s \t%08x\n", opcode, bytes, mnemonic, control_word));
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

    static private String toFlags(int flags) {
        String f = "";
        if ((flags & 0x01) == 0) f += "c"; else f+="C";
        if ((flags & 0x02) == 0) f += "v"; else f+="V";
        if ((flags & 0x04) == 0) f += "z"; else f+="Z";
        if ((flags & 0x08) == 0) f += "n"; else f+="N";
        return f;
    }

    public static void main(String[] args) {
        // TODO: Read ROMs first and compare to indicate which ROMs changed
        BufferedWriter bw;
        System.out.println("Creating Control Signals for Pip pipelined CPU");
        try {
            bw = new BufferedWriter(new FileWriter("opcodes", false));
            for (int opcode = 0; opcode < 0x100; opcode++) {
                for (int flags = 0; flags < 16; flags++) {
                    int control_word = do_instruction(opcode, flags, bw);
                    System.out.printf("%02x %s %08x   %s\n", opcode, toFlags(flags), control_word, toBinary(control_word));
                    int addr = opcode | flags << 8;
                    ctrl1b[addr] = (char) ((control_word) & 0xff);
                    ctrl1a[addr] = (char) ((control_word >> 8) & 0xff);
                    ctrl2b[addr] = (char) ((control_word >> 16) & 0xff);
                    ctrl2a[addr] = (char) ((control_word >> 24) & 0xff);
                }
            }
            bw.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
        // Write ROMS
        write_bytes("ctrl1a.bin", ctrl1a);
        write_bytes("ctrl1b.bin", ctrl1b);
        write_bytes("ctrl2a.bin", ctrl2a);
        write_bytes("ctrl2b.bin", ctrl2b);
    }
}