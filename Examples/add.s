# First program
    ORG $0000
start:
    NOP
    MOV B,'`'
loop:
    IN A
    CMP A
    JEQ loop
    OUT A # Output A to terminal
    CMP A,B
    JNE loop
    HLT
