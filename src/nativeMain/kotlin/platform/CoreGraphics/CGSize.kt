package platform.CoreGraphics

import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents

val CValue<CGSize>.width get() = this.useContents { width }
val CValue<CGSize>.height get() = this.useContents { height }
