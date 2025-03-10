package be.sgl.backend.service

import be.sgl.backend.dto.RoleDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.Role
import be.sgl.backend.mapper.RoleMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.service.exception.RoleNotFoundException
import be.sgl.backend.service.user.UserDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RoleService {

    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var mapper: RoleMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var branchRepository: BranchRepository

    fun getAllRoles(): List<RoleDTO> {
        return roleRepository.findAll().map(mapper::toDto)
    }

    fun saveRoleDTO(dto: RoleDTO): RoleDTO {
        return mapper.toDto(roleRepository.save(mapper.toEntity(dto)))
    }

    fun mergeRoleDTOChanges(id: Int, dto: RoleDTO): RoleDTO {
        val role = getRoleById(id)
        role.name = dto.name
        role.externalId = dto.externalId
        role.backupExternalId = dto.backupExternalId
        role.branch = dto.branch?.id?.let { getBranchById(it) }
        role.staffBranch = dto.staffBranch?.id?.let { getBranchById(it) }
        role.level = dto.level
        return mapper.toDto(roleRepository.save(role))
    }

    fun deleteRole(id: Int) {
        roleRepository.delete(getRoleById(id))
    }

    fun assignRoleToUser(id: Int, username: String) {
        val role = getRoleById(id)
        val user = userDataProvider.getUser(username)
        userDataProvider.startRole(user, role)
    }

    fun deassignRoleFromUser(id: Int, username: String) {
        val role = getRoleById(id)
        val user = userDataProvider.getUser(username)
        userDataProvider.endRole(user, role)
    }

    private fun getRoleById(id: Int): Role {
        return roleRepository.findById(id).orElseThrow { RoleNotFoundException() }
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}