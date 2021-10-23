package huancun.prefetch

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import freechips.rocketchip.tilelink._
import huancun._

class PrefetchReq(implicit p: Parameters) extends PrefetchBundle {
  // val addr = UInt(addressBits.W)
  val tag = UInt(tagBits.W)
  val set = UInt(setBits.W)
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  val alias = if (hasAliasBits) Some(UInt(aliasBitsOpt.get.W)) else None
  // val id = UInt(sourceIdBits.W)
}

class PrefetchResp(implicit p: Parameters) extends PrefetchBundle {
  // val id = UInt(sourceIdBits.W)
  val tag = UInt(tagBits.W)
  val set = UInt(setBits.W)
  def addr = Cat(tag, set, 0.U(offsetBits.W))
}

class PrefetchTrain(implicit p: Parameters) extends PrefetchBundle {
  // val addr = UInt(addressBits.W)
  val tag = UInt(tagBits.W)
  val set = UInt(setBits.W)
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  val alias = if (hasAliasBits) Some(UInt(aliasBitsOpt.get.W)) else None
  // prefetch only when L2 receives a miss or prefetched hit req
  // val miss = Bool()
  // val prefetched = Bool()

  val vaddr = UInt(cacheParams.clientCaches.head.vaddrBits.W)

  def addr = Cat(tag, set, 0.U(offsetBits.W))
}

class PrefetchUpdate(implicit p: Parameters) extends PrefetchBundle {
  val commit = new CoreCommitInfos
}

class PrefetchIO(implicit p: Parameters) extends PrefetchBundle {
  val update = Flipped(new PrefetchUpdate)
  val train = Flipped(DecoupledIO(new PrefetchTrain))
  val req = DecoupledIO(new MSHRRequest)
  val resp = Flipped(DecoupledIO(new PrefetchResp))
}

class PrefetchQueue[T <: Data](val gen: T, val entries: Int)(implicit p: Parameters) extends PrefetchModule {
  val io = IO(new Bundle {
    val enq = Flipped(DecoupledIO(gen))
    val deq = DecoupledIO(gen)
  })
  /*  Here we implement a queue that
   *  1. is pipelined  2. flows
   *  3. always has the latest reqs, which means the queue is always ready for enq and deserting the eldest ones
   */
  val queue = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(gen))))
  val valids = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val idxWidth = log2Up(entries)
  val head = RegInit(0.U(idxWidth.W))
  val tail = RegInit(0.U(idxWidth.W))
  val empty = head === tail && !valids.last
  val full = head === tail && valids.last

  when(!empty && io.deq.ready) {
    valids(head) := false.B
    head := head + 1.U
  }

  when(io.enq.valid) {
    queue(tail) := io.enq.bits
    valids(tail) := !empty || !io.deq.ready // true.B
    tail := tail + (!empty || !io.deq.ready).asUInt
    when(full && !io.deq.ready) {
      head := head + 1.U
    }
  }

  io.enq.ready := true.B
  io.deq.valid := !empty || io.enq.valid
  io.deq.bits := Mux(empty, io.enq.bits, queue(head))
}

class Prefetcher(implicit p: Parameters) extends PrefetchModule {
  val io = IO(new PrefetchIO)

  prefetchOpt.get match {
    case bop: BOPParameters =>
      val pft = Module(new BestOffsetPrefetch)
      val pftQueue = Module(new PrefetchQueue(new PrefetchReq, inflightEntries))
      pft.io.train <> io.train
      pft.io.resp <> io.resp
      pftQueue.io.enq <> pft.io.req
      pftQueue.io.deq.ready := io.req.ready
      io.req.valid := pftQueue.io.deq.valid
      io.req.bits.opcode := TLMessages.Hint
      io.req.bits.param := Mux(pftQueue.io.deq.bits.needT, TLHints.PREFETCH_WRITE, TLHints.PREFETCH_READ)
      io.req.bits.size := log2Up(blockBytes).U
      io.req.bits.source := pftQueue.io.deq.bits.source
      io.req.bits.set := pftQueue.io.deq.bits.set
      io.req.bits.tag := pftQueue.io.deq.bits.tag
      io.req.bits.off := 0.U
      io.req.bits.channel := "b001".U
      io.req.bits.needHint.foreach(_ := false.B)
      io.req.bits.isPrefetch.foreach(_ := true.B)
      io.req.bits.alias.foreach(_ := pftQueue.io.deq.bits.alias.get)
      io.req.bits.preferCache := true.B
      io.req.bits.fromProbeHelper := false.B
      io.req.bits.bufIdx := DontCare
      io.req.bits.dirty := false.B
      io.req.bits.needProbeAckData.foreach(_ := false.B)
    case pc: PCParameters =>

    case _ => assert(cond = false, "Unknown prefetcher")
  }
}
