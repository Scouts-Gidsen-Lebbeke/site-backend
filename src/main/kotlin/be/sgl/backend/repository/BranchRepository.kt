package be.sgl.backend.repository

import be.sgl.backend.entity.Branch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : JpaRepository<Branch, Int>