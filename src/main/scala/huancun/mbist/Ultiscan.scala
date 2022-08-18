/** *************************************************************************************
  * Copyright (c) 2020-2022 Institute of Computing Technology, Chinese Academy of Sciences
  * Copyright (c) 2020-2022 Peng Cheng Laboratory
  *
  * XiangShan is licensed under Mulan PSL v2.
  * You can use this software according to the terms and conditions of the Mulan PSL v2.
  * You may obtain a copy of Mulan PSL v2 at:
  *          http://license.coscl.org.cn/MulanPSL2
  *
  * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
  * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
  * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
  *
  * See the Mulan PSL v2 for more details.
  * *************************************************************************************
  */

package huancun.mbist

import chisel3._
import huancun.utils.DFTResetGen


class UltiscanExternalInterface extends Bundle{
  val mode = Input(Bool())

  val byprst_b = Input(Bool())
  val clkungate = Input(Bool())
  val clkungate_syn = Input(Bool())
  val rstbypen = Input(Bool())
  val core_shiften = Input(Bool())

  val ram = new Bundle () {
    val bypsel = Input(Bool())
    val hold = Input(Bool())
    val init_en = Input(Bool())
    val init_val = Input(Bool())
    val mcp = Input(Bool())
    val odis_b = Input(Bool())
    val rddis_b = Input(Bool())
    val wrdis_b = Input(Bool())
  }

  def toResetGen: DFTResetGen = {
    val top_scan = Wire(new DFTResetGen)
    top_scan.scan_mode := rstbypen
    top_scan.dft_reset := !byprst_b
    top_scan.dft_mode := rstbypen
    top_scan
  }
}
class UltiscanJTAGInterface extends Bundle {

  val capture = Input(Bool())
  val reset_b = Input(Bool())
  val select = Input(Bool())
  val shift = Input(Bool())
  val si = Input(Bool())
  val tck = Input(Bool())
  val update = Input(Bool())
  val so = Output(Bool())
}

class UltiscanUscanInterface (
  NUM_CHANNELS_IN: Int,
  NUM_CHANNELS_OUT: Int
) extends Bundle {
  val state = Input(Bool())
  val edt_update = Input(Bool())
  val mode = Input(Bool())
  val scanclk = Input(Bool())
  val si = Input(UInt(NUM_CHANNELS_IN.W))
  val so = Output(UInt(NUM_CHANNELS_OUT.W))
}

class UltiscanIO (
  NUM_CHAINS: Int,
  NUM_CHANNELS_IN: Int,
  NUM_CHANNELS_OUT: Int,
  NUM_CLKGENCTRL: Int,
  NUM_CLKGENCTRLEN: Int
) extends Bundle {
  val fscan = new Bundle() {
    val mode = Output(Bool())
    val mode_atspeed = Output(Bool())
    val state = Output(Bool())


    val byplatrst_b = Output(Bool())
    val byprst_b = Output(Bool())
    val clkgenctrl = Output(UInt(NUM_CLKGENCTRL.W))
    val clkgenctrlen = Output(UInt(NUM_CLKGENCTRLEN.W))
    val clkungate = Output(Bool())
    val clkungate_syn = Output(Bool())
    val rstbypen = Output(Bool())
    val shiften = Output(Bool())

    val ram = new Bundle () {
      val bypsel = Output(Bool())
      val hold = Output(Bool())
      val init_en = Output(Bool())
      val init_val = Output(Bool())
      val mcp = Output(Bool())
      val odis_b = Output(Bool())
      val rddis_b = Output(Bool())
      val wrdis_b = Output(Bool())
    }
  }
  val fdfx_powergood = Input(Bool())
  val ijtag = new UltiscanJTAGInterface

  val scanchains_so_end = Input(UInt((NUM_CHAINS - 2).W))
  val scanchains_si_bgn = Output(UInt((NUM_CHAINS - 2).W))

  val dftclken = Output(Bool())
  val core_clock_preclk = Input(Clock())
  val core_clock_postclk = Output(Clock())

  val uscan = new UltiscanUscanInterface(NUM_CHANNELS_IN, NUM_CHANNELS_OUT)
}


class Ultiscan (
  NUM_CHAINS: Int,
  NUM_CHANNELS_IN: Int,
  NUM_CHANNELS_OUT: Int,
  NUM_CLKGENCTRL: Int,
  NUM_CLKGENCTRLEN: Int,
  RSTVAL_CLKGENCTRL: Int,
  RSTVAL_CLKGENCTRLEN: Int,
  prefix: String,
  sim: Boolean = true
) extends RawModule {
  override val desiredName = prefix + "_ultiscan_top"

  val io = IO(new UltiscanIO(
    NUM_CHAINS,
    NUM_CHANNELS_IN,
    NUM_CHANNELS_OUT,
    NUM_CLKGENCTRL,
    NUM_CLKGENCTRLEN
  ))
  dontTouch(io)
  io.suggestName(prefix)

  io := DontCare
  io.core_clock_postclk := io.core_clock_preclk

  def toResetGen: DFTResetGen = {
    val top_scan = Wire(new DFTResetGen)
    top_scan.scan_mode := io.fscan.rstbypen
    top_scan.dft_reset := !io.fscan.byprst_b
    top_scan.dft_mode := io.fscan.rstbypen
    top_scan
  }
}