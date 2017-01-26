

/**
  * Created by jens on 26.01.17.
  */
object AnsiCodeParser {
  //
  //  val ansiToHtml = Map(
  //    0 -> "black",
  //    30 -> "black",
  //    31 -> "maroon",
  //    32 -> "green",
  //    33 -> "olive",
  //    34 -> "navy",
  //    35 -> "purple",
  //    36 -> "teal",
  //    37 -> "silver"
  //  )
  //
  //  def ansiCode = "\\e\\[".r ~> number <~ "m".r
  //
  //  def number = "\\d+".r ^^ { _.toInt }
  //
  //  def ansiText = ansiCode ~ text ^^ {
  //    case codeStart ~ text => s"""<span style="color: ${ansiToHtml(codeStart)}">$text</span>"""
  //  }
  //
  //  def newline = "\\n?".r ^^ { _ => "" }
  //
  //  def character = """\\W""".r ^^ { _.toString }
  //
  //  def text = (character ~ guard(ansiCode)).* ^^ { _.mkString }


  val ansiRegex = "\\e\\[\\d+m".r

//  def ansiToHtml(input: String): String = {
    //    parse(ansiText, input)  match {
    //      case Success(matched, _) => matched.mkString("")
    //      case Failure(msg, _) => msg
    //      case Error(msg, _) => msg
    //    }
    // Simply replace ansi codes for now
//  }

}
