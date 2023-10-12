# First program
    ORG $0000
start:
    NOP
    MOV T,0
    MOV B,'['
    MOV A,'A'
loop:
    OUT A # Output A to terminal
    INC A
    CMP A,B
    JNE loop
    OUT '-'
    JMP start
    HLT
