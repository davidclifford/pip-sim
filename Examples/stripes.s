    ORG $0000
    NOP
start:
wait_key:
    IN A
    CMP A
    JEQ wait_key

    MOV H,>y
    MOV L,y
    STO 0

loop2:
    MOV H,>x
    MOV L,x
    STO 0

loop1:
    MOV H,>y
    MOV L,y
    LDA

    MOV H,>x
    MOV L,x
    LDB

    MOV H,A
    MOV L,B

    STO $3c

    INC B
    MOV H,>x
    MOV L,x
    STO B

    MOV A,160
    CMP A,B

    MOV T,>loop1
    JNE loop1

    MOV H,>y
    MOV L,y
    LDB
    INC B
    STO B
    MOV A,120
    CMP A,B
    JNE loop2

stop:
    JMP stop

ORG $f000

x:  db 0
y:  db 0
c:  db 0
