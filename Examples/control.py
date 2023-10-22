############################
#
# PIPE1
#
# LOAD_CONSTANT (1-bit)
# NO_FETCH (1-bit)
# 2 - bits
#
# PIPE2
#
# ALU OP (5-bits)
# ALU IN (1-bit) A/B or C/D
# DATA_ASSERT (3-bits)
# DATA_READ (4-bits)
# ADDRESS_ASSERT (2-bit)
# BUS_REQ (1-bit)
#
# 16 - bits
############################

# -- PIPE 1 --
# LOAD_CONSTANT

# -- PIPE 2 --
# ALU OP (5-bits)
# REG SEL (1-bit)
# BUS_REQ (1-bit)

# ASSERT TO DATA BUS (3-bits)
# 00 None
# 01 MEM
# 02 K
# 03 ALU (E)
# 04 I/O
# 05 I/O CTRL

# READ FROM DATA BUS (4-bits)
# 00 None
# 01 MEM
# 02 A
# 03 B
# 04 C
# 05 D
# 06 I/O
# 07 T
# 08 PC
# 09 BANK
# 0a H0
# 0b L0
# 0c H1
# 0d L1
# 0e H2
# 0f L2

# ADDRESS BUS (2-bits)
# 00 PC
# 01 HL0
# 02 HL1
# 03 HL2

# JSR
#     MOV L0,$FE # Return address store High
#     MOV H0,$00 # Return address store Low
#     STO HL0,$01  # Store return address High in RAM
#     MOV H0,$01 # Return address store Low
#     ST0 HL0,$11  # Store return address Low in RAM
#     MOV T,$03   # Subroutine address High
#     JMP $2f     # Subroutine address low - Jump to Subroutine
#     (14 bytes/cycles)
#
# RET
#     MOV H0,$FE
#     MOV L0,$00 # M0 := $FE00
#     LDT HL0    # T := $01
#     MOV L0,$01 #
#     LDP HL0    # PC := $0111
#     (8 Bytes/cycles)

# STRIPES
#     CLR A # pixel
#     CLR B # y
# next_y:
#     CLR C # x
# next_x:
#     MOV H0,B
#     MOV L0,C
#     STO HL0,A
#     INC A
#     INC C
#     MOV D,160
#     CMP C,D
#     MOV T,>next_x
#     JNE next_x
#     INC B
#     MOV A,120
#     CMP A,B
#     MOV T,>next_y
#     JNE next_y
#     HLT

# Draw Pixel at x,y
# 16 byte/cycles
#   MOV H0,>x
#   MOV L0,x
#   LDH0 HL0

#   MOV H1,>y
#   MOV L1,y
#   LDL0 HL1

#   MOV H1,>pixel
#   MOV L1,pixel
#   LDA HL1
#   STA HL0

# 9 bytes - 17 cycles
# LDA pixel (5)
# LDB x     (5)
# STI A y,B (7)

