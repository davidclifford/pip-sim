org $0000
    NOP
    OUT 'A'
    JSR out_z
    OUT 'B'
    JSR out_z
    OUT 'C'
    HLT

out_z:
    OUT 'Z'
    RTS out_z
    NOP
