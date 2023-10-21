# First program
start:
    ORG $0000
    NOP
    MOV H,>ptr
    MOV L,ptr
    MOV B,buff
    STO B
next:
    IN A
    CMP A
    JEQ next
    OUT A

    MOV H,>buff
    MOV L,B
    STO A

    MOV B,10
    CMP A,B
    JEQ print

    MOV H,>ptr
    MOV L,ptr
    LDB
    INC B
    STO B

    JMP next

print:
    MOV H,>ptr
    MOV L,ptr
    STO buff
    MOV B,buff
print_loop:
    MOV H,>buff
    MOV L,B
    LDA
    OUT A
    MOV B,10
    CMP A,B
    JEQ stop
    MOV H,>ptr
    MOV L,ptr
    LDB
    INC B
    STO B
    JMP print_loop
stop:
    HLT

ORG $ff00
ptr: db
buff: db (20)


