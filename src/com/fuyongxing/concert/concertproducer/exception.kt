package com.fuyongxing.concert.concertproducer

class ConcertHallAPIException(override val message: String?="访问音乐厅API失败") : Exception(message)