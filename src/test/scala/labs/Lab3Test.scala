// Tests for Lab 3. Feel free to modify and add more tests here.
// If you name your test class something that ends with "TesterLab4" it will
// automatically be run when you use `Lab4 / test` at the sbt prompt.


package dinocpu

import chisel3._

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly dinocpu.RTypeTesterLab4
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly dinocpu.RTypeTesterLab4'
  * }}}
  */
class RTypeTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.rtype) {
    it should s"run R-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class ITypeTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.itype) {
    it should s"run I-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class UTypeTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.utype) {
    it should s"run U-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class MemoryTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.memory) {
    it should s"run memory-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class RTypeMultiCycleTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.rtypeMultiCycle) {
    it should s"run multi cycle R-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class ITypeMultiCycleTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.itypeMultiCycle) {
    it should s"run multi cycle I-type instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class BranchTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.branch) {
    it should s"run branch instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class JumpTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.jump) {
    it should s"run jump instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class MemoryMultiCycleTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.memoryMultiCycle) {
    it should s"run multi cycle memory instruction ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

class ApplicationsTesterLab4 extends CPUFlatSpec {
  behavior of "Pipelined CPU"
  for (test <- InstTests.applications) {
    it should s"run application ${test.binary}${test.extraName}" in {
      CPUTesterDriver(test, "pipelined") should be(true)
    }
  }
}

