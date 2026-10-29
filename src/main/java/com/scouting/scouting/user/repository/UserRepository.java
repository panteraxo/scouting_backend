package com.scouting.scouting.user.repository;

import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * Recupera la cuenta bloqueando su fila hasta que termine la transaccion.
     * Se usa para que solo una operacion procese una solicitud pendiente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

    boolean existsByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);

    /**
     * Bloquea en orden determinista todas las cuentas ADMIN activas. Se usa como
     * primer paso de las operaciones de asignacion/estado para que dos
     * degradaciones simultaneas no puedan dejar el sistema sin administradores.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = :role and u.status = :status order by u.id asc")
    List<User> findByRoleAndStatusForUpdate(@Param("role") Role role, @Param("status") UserStatus status);

    /**
     * Listado administrativo. Solo admite ACTIVE/INACTIVE (el servicio excluye
     * PENDING_APPROVAL). {@code qPattern} llega ya escapado y envuelto en
     * {@code %} para que {@code %}, {@code _} y {@code !} se busquen literalmente.
     */
    @Query("""
            select u from User u
            where u.status in :statuses
              and (:role is null or u.role = :role)
              and (:academyId is null or exists (
                    select uo.id from UserOrganization uo
                    where uo.user = u and uo.academy.id = :academyId))
              and (:clubId is null or exists (
                    select uo.id from UserOrganization uo
                    where uo.user = u and uo.club.id = :clubId))
              and (:qPattern is null
                   or lower(u.firstName) like lower(:qPattern) escape '!'
                   or lower(u.lastName) like lower(:qPattern) escape '!'
                   or lower(u.username) like lower(:qPattern) escape '!'
                   or lower(u.email) like lower(:qPattern) escape '!'
                   or lower(u.documentNumber) like lower(:qPattern) escape '!')
            """)
    Page<User> searchAdmin(@Param("statuses") Collection<UserStatus> statuses,
                           @Param("role") Role role,
                           @Param("academyId") Long academyId,
                           @Param("clubId") Long clubId,
                           @Param("qPattern") String qPattern,
                           Pageable pageable);
}
