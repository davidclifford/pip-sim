org $0000
    NOP
loop:
    OUT 'A'
    JSR out_z
    OUT 'B'
    JSR out_z
    OUT 'C'
    OUT 10
    JMP loop

out_z:
    OUT 'Z'
    RTS out_z
    NOP
