package models

case class Center(
                   centerCode: String,
                   centerName: String,
                   postalAddress: String,
                   phone: String,
                   centerEmail: String,
                   generalDirectorName: String,
                   hrManager: String,
                   accountingManager: String,
                   secretaryName: String,
                   logo: Option[String]
                 ) {
  def isValid: Boolean =
    centerCode.nonEmpty &&
      centerName.nonEmpty &&
      centerEmail.contains("@")
}
