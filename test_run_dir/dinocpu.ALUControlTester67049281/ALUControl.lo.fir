circuit ALUControl :
  module ALUControl :
    input clock : Clock
    input reset : UInt<1>
    input io_add : UInt<1>
    input io_immediate : UInt<1>
    input io_funct7 : UInt<7>
    input io_funct3 : UInt<3>
    output io_operation : UInt<4>
  
    node _T = eq(UInt<1>("h0"), io_funct3) @[Conditional.scala 37:30]
    node _T_1 = eq(io_funct7, UInt<1>("h0")) @[alucontrol.scala 37:41]
    node _T_2 = or(io_immediate, _T_1) @[alucontrol.scala 37:28]
    node _GEN_0 = mux(_T_2, UInt<2>("h2"), UInt<2>("h3")) @[alucontrol.scala 37:59]
    node _T_3 = eq(UInt<1>("h1"), io_funct3) @[Conditional.scala 37:30]
    node _T_4 = eq(UInt<2>("h2"), io_funct3) @[Conditional.scala 37:30]
    node _T_5 = eq(UInt<2>("h3"), io_funct3) @[Conditional.scala 37:30]
    node _T_6 = eq(UInt<3>("h4"), io_funct3) @[Conditional.scala 37:30]
    node _T_7 = eq(UInt<3>("h5"), io_funct3) @[Conditional.scala 37:30]
    node _T_8 = eq(io_funct7, UInt<1>("h0")) @[alucontrol.scala 48:25]
    node _GEN_1 = mux(_T_8, UInt<3>("h7"), UInt<4>("h8")) @[alucontrol.scala 48:43]
    node _T_9 = eq(UInt<3>("h6"), io_funct3) @[Conditional.scala 37:30]
    node _T_10 = eq(UInt<3>("h7"), io_funct3) @[Conditional.scala 37:30]
    node _GEN_2 = mux(_T_10, UInt<1>("h0"), UInt<4>("hf")) @[Conditional.scala 39:67]
    node _GEN_3 = mux(_T_9, UInt<1>("h1"), _GEN_2) @[Conditional.scala 39:67]
    node _GEN_4 = mux(_T_7, _GEN_1, _GEN_3) @[Conditional.scala 39:67]
    node _GEN_5 = mux(_T_6, UInt<4>("h9"), _GEN_4) @[Conditional.scala 39:67]
    node _GEN_6 = mux(_T_5, UInt<3>("h5"), _GEN_5) @[Conditional.scala 39:67]
    node _GEN_7 = mux(_T_4, UInt<3>("h4"), _GEN_6) @[Conditional.scala 39:67]
    node _GEN_8 = mux(_T_3, UInt<3>("h6"), _GEN_7) @[Conditional.scala 39:67]
    node _GEN_9 = mux(_T, _GEN_0, _GEN_8) @[Conditional.scala 40:58]
    node _GEN_10 = mux(io_add, UInt<2>("h2"), _GEN_9) @[alucontrol.scala 32:17]
    io_operation <= _GEN_10 @[alucontrol.scala 30:16 alucontrol.scala 33:18 alucontrol.scala 38:24 alucontrol.scala 40:24 alucontrol.scala 43:36 alucontrol.scala 44:36 alucontrol.scala 45:36 alucontrol.scala 46:36 alucontrol.scala 49:24 alucontrol.scala 51:24 alucontrol.scala 54:36 alucontrol.scala 55:36]
