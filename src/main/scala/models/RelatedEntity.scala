package models

case class RelatedEntity(
                          entityName: String,
                          entityType: String,
                          address: String,
                          phone: String,
                          contactEmail: String,
                          directorName: String,
                          centerCode: String
                        ) {
  def isValid: Boolean =
    entityName.nonEmpty &&
      entityType.nonEmpty &&
      contactEmail.contains("@")
}
