circuit BranchControl :
  module BranchControl :
    input clock : Clock
    input reset : UInt<1>
    input io_branch : UInt<1>
    input io_funct3 : UInt<3>
    input io_inputx : UInt<32>
    input io_inputy : UInt<32>
    output io_taken : UInt<1>
  
    node _T = eq(UInt<1>("h0"), io_funct3) @[Conditional.scala 37:30]
    node _T_1 = eq(io_inputx, io_inputy) @[branch-control.scala 33:40]
    node _T_2 = eq(UInt<1>("h1"), io_funct3) @[Conditional.scala 37:30]
    node _T_3 = neq(io_inputx, io_inputy) @[branch-control.scala 34:40]
    node _T_4 = eq(UInt<3>("h4"), io_funct3) @[Conditional.scala 37:30]
    node _T_5 = asSInt(io_inputx) @[branch-control.scala 35:40]
    node _T_6 = asSInt(io_inputy) @[branch-control.scala 35:59]
    node _T_7 = lt(_T_5, _T_6) @[branch-control.scala 35:47]
    node _T_8 = eq(UInt<3>("h5"), io_funct3) @[Conditional.scala 37:30]
    node _T_9 = asSInt(io_inputx) @[branch-control.scala 36:40]
    node _T_10 = asSInt(io_inputy) @[branch-control.scala 36:60]
    node _T_11 = geq(_T_9, _T_10) @[branch-control.scala 36:47]
    node _T_12 = eq(UInt<3>("h6"), io_funct3) @[Conditional.scala 37:30]
    node _T_13 = lt(io_inputx, io_inputy) @[branch-control.scala 37:40]
    node _T_14 = eq(UInt<3>("h7"), io_funct3) @[Conditional.scala 37:30]
    node _T_15 = geq(io_inputx, io_inputy) @[branch-control.scala 38:40]
    node _GEN_0 = validif(_T_14, _T_15) @[Conditional.scala 39:67]
    node _GEN_1 = mux(_T_12, _T_13, _GEN_0) @[Conditional.scala 39:67]
    node _GEN_2 = mux(_T_8, _T_11, _GEN_1) @[Conditional.scala 39:67]
    node _GEN_3 = mux(_T_4, _T_7, _GEN_2) @[Conditional.scala 39:67]
    node _GEN_4 = mux(_T_2, _T_3, _GEN_3) @[Conditional.scala 39:67]
    node _GEN_5 = mux(_T, _T_1, _GEN_4) @[Conditional.scala 40:58]
    node check = _GEN_5 @[branch-control.scala 29:19 branch-control.scala 33:26 branch-control.scala 34:26 branch-control.scala 35:26 branch-control.scala 36:26 branch-control.scala 37:26 branch-control.scala 38:26]
    node _T_16 = and(check, io_branch) @[branch-control.scala 41:21]
    io_taken <= _T_16 @[branch-control.scala 41:12]
