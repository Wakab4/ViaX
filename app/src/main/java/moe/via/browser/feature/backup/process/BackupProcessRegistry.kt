package moe.via.browser.feature.backup.process

/** Process-only fallback for framework Bundle restoration; passwords and files never enter the Bundle. */
internal object BackupProcessRegistry {
    private val owners = mutableMapOf<String, BackupProcessOwner>()
    fun register(owner: BackupProcessOwner) { owners[owner.token] = owner }
    fun find(token: String?): BackupProcessOwner? = token?.let(owners::get)?.takeUnless { it.closed }
    fun remove(owner: BackupProcessOwner) { if (owners[owner.token] === owner) owners.remove(owner.token) }
}
