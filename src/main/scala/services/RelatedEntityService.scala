package services

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import models.RelatedEntity

class RelatedEntityService[F[_]: MonadCancelThrow] (xa: Transactor[F]) {

  def create(entity: RelatedEntity): ConnectionIO[Either[String, RelatedEntity]] = {
    if (!entity.isValid) {
      MonadCancelThrow[ConnectionIO].pure(Left("Invalid entity data"))
    } else {
      val insert = sql"""
        INSERT INTO relatedentity (entityname, entitytype, address, phone, contactemail,
                                  directorname, centercode)
        VALUES (${entity.entityName}, ${entity.entityType}, ${entity.address},
                ${entity.phone}, ${entity.contactEmail}, ${entity.directorName}, ${entity.centerCode})
      """.update

      insert.run.map(_ => Right(entity))
    }
  }

  def findByName(entityName: String): ConnectionIO[Option[RelatedEntity]] = {
    sql"""
      SELECT entityname, entitytype, address, phone, contactemail, directorname, centercode
      FROM relatedentity
      WHERE entityname = $entityName
    """.query[RelatedEntity].option
  }

  def findAll: ConnectionIO[List[RelatedEntity]] = {
    sql"""
      SELECT entityname, entitytype, address, phone, contactemail, directorname, centercode
      FROM relatedentity
    """.query[RelatedEntity].to[List]
  }

  def update(entity: RelatedEntity): ConnectionIO[Either[String, RelatedEntity]] = {
    val updateSql = sql"""
      UPDATE relatedentity
      SET entitytype = ${entity.entityType}, address = ${entity.address},
          phone = ${entity.phone}, contactemail = ${entity.contactEmail},
          directorname = ${entity.directorName}, centercode = ${entity.centerCode}
      WHERE entityname = ${entity.entityName}
    """.update

    updateSql.run.map { affected =>
      if (affected > 0) Right(entity)
      else Left(s"Entity with name ${entity.entityName} not found")
    }
  }

  def delete(entityName: String): ConnectionIO[Either[String, Unit]] = {
    sql"DELETE FROM relatedentity WHERE entityname = $entityName".update.run.map { affected =>
      if (affected > 0) Right(())
      else Left(s"Entity with name $entityName not found")
    }
  }
}

object RelatedEntityService {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): RelatedEntityService[F] =
    new RelatedEntityService(xa)
}