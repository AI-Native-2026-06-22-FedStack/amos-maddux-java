package com.fedstack.spending.auth.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAuthCredentialsRepository extends JpaRepository<UserAuthCredentialsEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select credentials
			from UserAuthCredentialsEntity credentials
			join fetch credentials.user user
			where lower(user.email) = lower(:email)
			""")
	Optional<UserAuthCredentialsEntity> findByEmailForUpdate(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select credentials
			from UserAuthCredentialsEntity credentials
			join fetch credentials.user user
			where credentials.userId = :userId
			""")
	Optional<UserAuthCredentialsEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}
