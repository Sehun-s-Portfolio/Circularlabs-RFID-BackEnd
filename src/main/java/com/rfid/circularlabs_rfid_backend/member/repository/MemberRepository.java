package com.rfid.circularlabs_rfid_backend.member.repository;

import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
