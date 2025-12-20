// This file is where all of the CPU components are assembled into the whole CPU

package dinocpu

import chisel3._
import chisel3.util._

/**
 * The main CPU definition that hooks up all of the other components.
 *
 * For more information, see section 4.6 of Patterson and Hennessy
 * This follows figure 4.49
 */
class PipelinedCPU(implicit val conf: CPUConfig) extends Module {
  val io = IO(new CoreIO)

  // Bundles defining the pipeline registers and control structures

  // Everything in the register between IF and ID stages
  class IFIDBundle extends Bundle {
    val instruction = UInt(32.W)
    val pc          = UInt(32.W)
    val pcplusfour  = UInt(32.W)
  }

  // Control signals used in EX stage
  class EXControl extends Bundle {
    val add       = Bool()
    val immediate = Bool()
    val alusrc1   = UInt(2.W)
    val branch    = Bool()
    val jump      = UInt(2.W)
  }

  // Control signals used in MEM stage
  class MControl extends Bundle {
    val memwrite = Bool()
    val memread  = Bool()
    val taken    = Bool()
  }

  // Control signals used in WB stage
  class WBControl extends Bundle {
    val toreg    = UInt(2.W)
    val regwrite = Bool()
  }

  // Everything in the register between ID and EX stages
  class IDEXBundle extends Bundle {
    val instruction = UInt(32.W)
    val pc          = UInt(32.W)
    val pcplusfour  = UInt(32.W)
    val sextimm     = UInt(32.W)
    val funct7      = UInt(7.W)
    val funct3      = UInt(3.W)
    val readreg1    = UInt(5.W)
    val readreg2    = UInt(5.W)
    val readdata1   = UInt(32.W)
    val readdata2   = UInt(32.W)
    val excontrol   = new EXControl
    val mcontrol    = new MControl
    val wbcontrol   = new WBControl
  }

  // Everything in the register between EX and MEM stages
  class EXMEMBundle extends Bundle {
    val writedata   = UInt(32.W)
    val targetPc    = UInt(32.W)
    val pcplusfour  = UInt(32.W)
    val aluResult   = UInt(32.W)
    val instruction = UInt(32.W)
    val funct3      = UInt(3.W)
    val mcontrol    = new MControl
    val wbcontrol   = new WBControl
  }

  // Everything in the register between MEM and WB stages
  class MEMWBBundle extends Bundle {
    val pcplusfour   = UInt(32.W)
    val aluResult    = UInt(32.W)
    val memReadData  = UInt(32.W)
    val instruction  = UInt(32.W)
    val wbcontrol    = new WBControl
  }

  // All of the structures required
  val pc         = RegInit(0.U)
  val control    = Module(new Control())
  val branchCtrl = Module(new BranchControl())
  val registers  = Module(new RegisterFile())
  val aluControl = Module(new ALUControl())
  val alu        = Module(new ALU())
  val immGen     = Module(new ImmediateGenerator())
  val pcPlusFour = Module(new Adder())
  val branchAdd  = Module(new Adder())
  val forwarding = Module(new ForwardingUnit())  //pipelined only
  val hazard     = Module(new HazardUnit())      //pipelined only
  val (cycleCount, _) = Counter(true.B, 1 << 30)

  val if_id      = RegInit(0.U.asTypeOf(new IFIDBundle))
  val id_ex      = RegInit(0.U.asTypeOf(new IDEXBundle))
  val ex_mem     = RegInit(0.U.asTypeOf(new EXMEMBundle))
  val mem_wb     = RegInit(0.U.asTypeOf(new MEMWBBundle))

  // Remove these as you hook up each one
  control.io    := DontCare
  branchCtrl.io := DontCare
  registers.io := DontCare
  aluControl.io := DontCare
  alu.io := DontCare
  immGen.io := DontCare
  pcPlusFour.io := DontCare
  branchAdd.io := DontCare
  io.dmem := DontCare
  forwarding.io := DontCare
  hazard.io := DontCare

  printf("Cycle=%d ", cycleCount)

  // Forward declaration of wires that connect different stages

  // From memory back to fetch. Since we don't decide whether to take a branch or not until the memory stage.
  val next_pc = Wire(UInt(32.W))
  next_pc    := DontCare    // remove when connected

  // For wb back to other stages
  val write_data = Wire(UInt(32.W))
  write_data    := DontCare // remove when connected

  /////////////////////////////////////////////////////////////////////////////
  // FETCH STAGE
  /////////////////////////////////////////////////////////////////////////////

  // Note: This comes from the memory stage!
  // Only update the pc if the pcwrite flag is enabled
  when (hazard.io.pcwrite === 1.U) {
    pc := next_pc
  } .elsewhen (hazard.io.pcwrite === 2.U) {
    pc := pc
  } .otherwise {
    pc := pcPlusFour.io.result
  }

  // Send the PC to the instruction memory port to get the instruction
  io.imem.address := pc

  // Get the PC + 4
  pcPlusFour.io.inputx := pc
  pcPlusFour.io.inputy := 4.U

  // Fill the IF/ID register if we are not bubbling IF/ID
  // otherwise, leave the IF/ID register *unchanged*
  when (hazard.io.pcwrite === 2.U) {
  } .elsewhen (hazard.io.ifid_flush === true.B) {
    if_id.instruction := 0.U
    if_id.pc          := 0.U
    if_id.pcplusfour  := 0.U
  } .otherwise {
    if_id.instruction := io.imem.instruction
    if_id.pc          := pc
    if_id.pcplusfour  := pcPlusFour.io.result
  }

  printf(p"IF/ID: $if_id\n")

  /////////////////////////////////////////////////////////////////////////////
  // ID STAGE
  /////////////////////////////////////////////////////////////////////////////

  val rs1 = if_id.instruction(19,15)
  val rs2 = if_id.instruction(24,20)

  // Send input from this stage to hazard detection unit
  hazard.io.rs1 := rs1
  hazard.io.rs2 := rs2

  // Send opcode to control
  control.io.opcode := if_id.instruction(6,0)

  // Send register numbers to the register file
  registers.io.readreg1 := rs1
  registers.io.readreg2 := rs2

  // Send the instruction to the immediate generator
  immGen.io.instruction := if_id.instruction
  when (hazard.io.idex_bubble === true.B) {
    // Set the execution control signals
    id_ex.excontrol.add := 0.U
    id_ex.excontrol.immediate := 0.U
    id_ex.excontrol.alusrc1 := 0.U
    id_ex.excontrol.branch := 0.U
    id_ex.excontrol.jump := 0.U

    // Set the memory control signals
    id_ex.mcontrol.memwrite := 0.U
    id_ex.mcontrol.memread := 0.U
    id_ex.mcontrol.taken := 0.U

    // Set the writeback control signals
    id_ex.wbcontrol.toreg := 0.U
    id_ex.wbcontrol.regwrite := 0.U
  } .otherwise {
    // Fill the id_ex register
    id_ex.instruction := if_id.instruction
    id_ex.pc := if_id.pc
    id_ex.pcplusfour := if_id.pcplusfour
    id_ex.sextimm := immGen.io.sextImm
    id_ex.funct7 := if_id.instruction(31,25)
    id_ex.funct3 := if_id.instruction(14,12)
    id_ex.readreg1 := rs1
    id_ex.readreg2 := rs2
    id_ex.readdata1 := registers.io.readdata1
    id_ex.readdata2 := registers.io.readdata2

    // Set the execution control signals
    id_ex.excontrol.add := control.io.add
    id_ex.excontrol.immediate := control.io.immediate
    id_ex.excontrol.alusrc1 := control.io.alusrc1
    id_ex.excontrol.branch := control.io.branch
    id_ex.excontrol.jump := control.io.jump

    // Set the memory control signals
    id_ex.mcontrol.memwrite := control.io.memwrite
    id_ex.mcontrol.memread := control.io.memread
    id_ex.mcontrol.taken := DontCare

    // Set the writeback control signals
    id_ex.wbcontrol.toreg := control.io.toreg
    id_ex.wbcontrol.regwrite := control.io.regwrite
  }

  printf("DASM(%x)\n", if_id.instruction)
  printf(p"ID/EX: $id_ex\n")

  /////////////////////////////////////////////////////////////////////////////
  // EX STAGE
  /////////////////////////////////////////////////////////////////////////////

  // Set the inputs to the hazard detection unit from this stage (SKIP FOR PART I)
  hazard.io.idex_memread := id_ex.mcontrol.memread
  hazard.io.idex_rd      := id_ex.instruction(11,7)

  // Set the input to the forwarding unit from this stage (SKIP FOR PART I)
  forwarding.io.rs1 := id_ex.readreg1
  forwarding.io.rs2 := id_ex.readreg2

  // Connect the ALU control wires (line 45 of single-cycle/cpu.scala)
  aluControl.io.add       := id_ex.excontrol.add
  aluControl.io.immediate := id_ex.excontrol.immediate
  aluControl.io.funct7    := id_ex.funct7
  aluControl.io.funct3    := id_ex.funct3

  // Insert the forward inputx mux here (SKIP FOR PART I)
  val fA = Wire(UInt())
  when (forwarding.io.forwardA === 1.U) {
    fA := ex_mem.aluResult
  } .elsewhen (forwarding.io.forwardA === 2.U) {
    fA := write_data
  } .otherwise {
    fA := id_ex.readdata1
  }

  // Insert the ALU inpux mux here (line 59 of single-cycle/cpu.scala)
  val alu_inputx = Wire(UInt())
  alu_inputx := DontCare
  switch(id_ex.excontrol.alusrc1) {
    is(0.U) { alu_inputx := fA }
    is(1.U) { alu_inputx := 0.U }
    is(2.U) { alu_inputx := id_ex.pc }
  }

  // Insert forward inputy mux here (SKIP FOR PART I)
  val fB = Wire(UInt())
  when (forwarding.io.forwardB === 1.U) {
    fB := ex_mem.aluResult
  } .elsewhen (forwarding.io.forwardB === 2.U) {
    fB := write_data
  } .otherwise {
    fB := id_ex.readdata2
  }

  // Input y mux (line 66 of single-cycle/cpu.scala)
  val alu_inputy = Mux(id_ex.excontrol.immediate, id_ex.sextimm, fB)
  alu.io.inputx := alu_inputx
  alu.io.inputy := alu_inputy

  // Connect the branch control wire (line 54 of single-cycle/cpu.scala)
  branchCtrl.io.branch := id_ex.excontrol.branch
  branchCtrl.io.funct3 := id_ex.funct3
  branchCtrl.io.inputx := id_ex.readdata1
  branchCtrl.io.inputy := id_ex.readdata2

  // Set the ALU operation
  alu.io.operation := aluControl.io.operation

  // Connect the branchAdd unit
  branchAdd.io.inputx := id_ex.pc
  branchAdd.io.inputy := id_ex.sextimm

  when (hazard.io.exmem_bubble === true.B){
    ex_mem.mcontrol.memwrite := 0.U
    ex_mem.mcontrol.memread := 0.U
    ex_mem.mcontrol.taken := 0.U

    ex_mem.wbcontrol.toreg := 0.U
    ex_mem.wbcontrol.regwrite := 0.U
    ex_mem.targetPc := 0.U
  } .otherwise {
    // Set the EX/MEM register values
    ex_mem.writedata := id_ex.readdata2
    ex_mem.targetPc  := DontCare
    ex_mem.pcplusfour := id_ex.pcplusfour
    ex_mem.aluResult := alu.io.result
    ex_mem.instruction := id_ex.instruction
    ex_mem.funct3 := id_ex.funct3

    ex_mem.mcontrol.memwrite := id_ex.mcontrol.memwrite
    ex_mem.mcontrol.memread := id_ex.mcontrol.memread

    ex_mem.wbcontrol.toreg := id_ex.wbcontrol.toreg
    ex_mem.wbcontrol.regwrite := id_ex.wbcontrol.regwrite

    // Calculate whether which PC we should use and set the taken flag (line 92 in single-cycle/cpu.scala)
    when (branchCtrl.io.taken || id_ex.excontrol.jump === 2.U) {
      ex_mem.targetPc := branchAdd.io.result
      ex_mem.mcontrol.taken := true.B
    } .elsewhen (id_ex.excontrol.jump === 3.U) {
      ex_mem.targetPc := alu.io.result & Cat(Fill(31, 1.U), 0.U)
      ex_mem.mcontrol.taken := true.B
    } .otherwise {
      ex_mem.targetPc := id_ex.pcplusfour
      ex_mem.mcontrol.taken := false.B
    }
  }
  printf(p"EX/MEM: $ex_mem\n")

  /////////////////////////////////////////////////////////////////////////////
  // MEM STAGE
  /////////////////////////////////////////////////////////////////////////////

  // Set data memory IO (line 71 of single-cycle/cpu.scala)
  io.dmem.address   := ex_mem.aluResult
  io.dmem.writedata := ex_mem.writedata
  io.dmem.memread   := ex_mem.mcontrol.memread
  io.dmem.memwrite  := ex_mem.mcontrol.memwrite
  io.dmem.maskmode  := ex_mem.funct3(1,0)
  io.dmem.sext      := ~ex_mem.funct3(2)

  // Send next_pc back to the fetch stage
  next_pc := ex_mem.targetPc

  // Send input signals to the hazard detection unit (SKIP FOR PART I)
  hazard.io.exmem_taken  := ex_mem.mcontrol.taken

  // Send input signals to the forwarding unit (SKIP FOR PART I)
  forwarding.io.exmemrw := ex_mem.wbcontrol.regwrite
  forwarding.io.exmemrd := ex_mem.instruction(11,7)

  // Wire the MEM/WB register
  mem_wb.pcplusfour := ex_mem.pcplusfour
  mem_wb.aluResult := ex_mem.aluResult
  mem_wb.memReadData := io.dmem.readdata
  mem_wb.instruction := ex_mem.instruction

  mem_wb.wbcontrol.toreg := ex_mem.wbcontrol.toreg
  mem_wb.wbcontrol.regwrite := ex_mem.wbcontrol.regwrite

  printf(p"MEM/WB: $mem_wb\n")

  /////////////////////////////////////////////////////////////////////////////
  // WB STAGE
  /////////////////////////////////////////////////////////////////////////////

  // Set the writeback data mux (line 78 single-cycle/cpu.scala)
  when (mem_wb.wbcontrol.toreg === 1.U) {
    write_data := mem_wb.memReadData
  } .elsewhen (mem_wb.wbcontrol.toreg === 2.U) {
    write_data := mem_wb.pcplusfour
  } .otherwise {
    write_data := mem_wb.aluResult
  }

  // Write the data to the register file
  registers.io.writereg  := mem_wb.instruction(11,7)
  registers.io.wen       := mem_wb.wbcontrol.regwrite && (registers.io.writereg =/= 0.U)
  registers.io.writedata := write_data

  // Set the input signals for the forwarding unit (SKIP FOR PART I)
  forwarding.io.memwbrw := mem_wb.wbcontrol.regwrite
  forwarding.io.memwbrd := mem_wb.instruction(11,7)

  printf("---------------------------------------------\n")
}
