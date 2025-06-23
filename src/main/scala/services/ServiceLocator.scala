package services

import cats.effect._

case class ServiceLocator[F[_]: MonadCancelThrow](
                                                   centerService: CenterService[F],
                                                   driverService: DriverService[F],
                                                   licenseService: LicenseService[F],
                                                   infractionService: InfractionService[F],
                                                   testService: TestService[F],
                                                   relatedEntityService: RelatedEntityService[F]
                                                 )

object ServiceLocator {
  def apply[F[_]: MonadCancelThrow](xa: doobie.util.transactor.Transactor[F]): ServiceLocator[F] =
    ServiceLocator(
      CenterService[F](xa),
      DriverService[F](xa),
      LicenseService[F](xa),
      InfractionService[F](xa),
      TestService[F](xa),
      RelatedEntityService[F](xa)
    )
}