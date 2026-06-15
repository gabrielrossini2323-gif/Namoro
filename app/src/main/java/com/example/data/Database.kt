package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. ENTITIES
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int,
    val bio: String,
    val occupation: String,
    val interestTags: String, // Comma-separated (e.g., "Música,Trilha,Café")
    val avatarIndex: Int,
    val isCurrentUser: Boolean = false,
    val distanceKm: Int = 5
)

@Entity(tableName = "swipe_actions")
data class SwipeAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val isLike: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_conversations")
data class MatchConversation(
    @PrimaryKey val id: String, // format: "match_{user1Id}_{user2Id}"
    val user1Id: Long,
    val user2Id: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: String,
    val senderId: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 2. DATA ACCESS OBJECT (DAO)
@Dao
interface CupidDao {
    @Query("SELECT * FROM user_profiles ORDER BY isCurrentUser DESC, id ASC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipe(swipe: SwipeAction)

    @Query("SELECT toUserId FROM swipe_actions WHERE fromUserId = :currentUserId")
    fun getSwipedUserIdsFlow(currentUserId: Long): Flow<List<Long>>

    @Query("SELECT toUserId FROM swipe_actions WHERE fromUserId = :currentUserId")
    suspend fun getSwipedUserIds(currentUserId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchConversation)

    @Query("SELECT * FROM match_conversations ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessage)

    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessage>>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfilesCount(): Int
}

// 3. DATABASE HOLDER
@Database(
    entities = [UserProfile::class, SwipeAction::class, MatchConversation::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class CupidDatabase : RoomDatabase() {
    abstract fun cupidDao(): CupidDao

    companion object {
        @Volatile
        private var INSTANCE: CupidDatabase? = null

        fun getDatabase(context: Context): CupidDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CupidDatabase::class.java,
                    "cupid_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 4. REPOSITORY
class CupidRepository(private val dao: CupidDao) {
    val allProfiles: Flow<List<UserProfile>> = dao.getAllProfiles()
    val currentUserProfile: Flow<UserProfile?> = dao.getCurrentUserProfile()
    val allMatches: Flow<List<MatchConversation>> = dao.getAllMatches()

    suspend fun getCurrentUser(): UserProfile? = dao.getCurrentUser()

    suspend fun insertProfile(profile: UserProfile) = dao.insertProfile(profile)
    suspend fun updateProfile(profile: UserProfile) = dao.updateProfile(profile)

    suspend fun swipe(fromUserId: Long, toUserId: Long, isLike: Boolean): Boolean {
        // Record swipe
        val swipe = SwipeAction(fromUserId = fromUserId, toUserId = toUserId, isLike = isLike)
        dao.insertSwipe(swipe)

        // Simulated MATCH condition:
        // Since we are offline, let's say there is a 70% chance of a match if we LIKE the user!
        if (isLike) {
            val rollMatch = (1..100).random() <= 70
            if (rollMatch) {
                val matchId = if (fromUserId < toUserId) "match_${fromUserId}_${toUserId}" else "match_${toUserId}_${fromUserId}"
                val match = MatchConversation(id = matchId, user1Id = fromUserId, user2Id = toUserId)
                dao.insertMatch(match)
                
                // Add an initial greeting message from the match
                val greetings = listOf(
                    "Oi! Tudo bem? Adorei seu perfil!",
                    "Olá! Que legal o seu match, como vai seu dia?",
                    "Oi! Vi que você gosta de coisas em comum comigo. Vamos conversar?",
                    "Olá! Match perfeito. Qual a boa de hoje?",
                    "Oi oi! Que bom ver que deu match, tudo bem?"
                )
                val initialMsg = greetings.random()
                dao.insertMessage(
                    ChatMessage(
                        matchId = matchId,
                        senderId = toUserId,
                        content = initialMsg,
                        timestamp = System.currentTimeMillis()
                    )
                )
                return true // Match was created!
            }
        }
        return false // No match created
    }

    suspend fun getSwipedUserIds(currentUserId: Long): List<Long> = dao.getSwipedUserIds(currentUserId)

    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessage>> = dao.getMessagesForMatch(matchId)

    suspend fun sendMessage(matchId: String, senderId: Long, content: String) {
        dao.insertMessage(
            ChatMessage(
                matchId = matchId,
                senderId = senderId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun seedDatabaseIfEmpty() {
        if (dao.getProfilesCount() == 0) {
            // Seed current user
            dao.insertProfile(
                UserProfile(
                    id = 999,
                    name = "Seu Nome",
                    age = 24,
                    bio = "Desenvolvedor Android. Gosto de tecnologia, café, shows de rock e aventuras na natureza. Mude meu perfil nas configurações!",
                    occupation = "Desenvolvedor",
                    interestTags = "Tecnologia,Café,Shows,Trilha",
                    avatarIndex = 0,
                    isCurrentUser = true,
                    distanceKm = 0
                )
            )

            // Seed lovely tinder swipe targets
            val list = listOf(
                UserProfile(
                    id = 1,
                    name = "Mariana",
                    age = 24,
                    bio = "Amo café gourmet, vinhedos e bons livros de suspense. Procuro alguém para dividir uma pizza napolitana e ótimas risadas.",
                    occupation = "Designer de Interiores",
                    interestTags = "Café,Livros,Arte,Pizza",
                    avatarIndex = 1,
                    distanceKm = 3
                ),
                UserProfile(
                    id = 2,
                    name = "Rodrigo",
                    age = 27,
                    bio = "Pratico escalada nos fins de semana e adoro trilhas selvagens pela floresta. Se você curte café da manhã com vista para as montanhas, deu match!",
                    occupation = "Fotógrafo de Viagens",
                    interestTags = "Trilha,Escalada,Fotografia,Ar Livre",
                    avatarIndex = 2,
                    distanceKm = 8
                ),
                UserProfile(
                    id = 3,
                    name = "Camila",
                    age = 25,
                    bio = "Total boba por cachorros, sushi e jogos de tabuleiro modernos. No fim de semana, ou estou inventando receitas novas ou com os pés na areia.",
                    occupation = "Chef de Cozinha",
                    interestTags = "Sushi,Cerveja,Cachorros,Praia",
                    avatarIndex = 3,
                    distanceKm = 5
                ),
                UserProfile(
                    id = 4,
                    name = "Lucas",
                    age = 29,
                    bio = "Músico aos fins de semana, programador por sobrevivência. Procuro alguém para ir a festivais de música indie, museus e maratona de animes.",
                    occupation = "Engenheiro de Software",
                    interestTags = "Música,Rock,Tecnologia,Animes",
                    avatarIndex = 4,
                    distanceKm = 12
                ),
                UserProfile(
                    id = 5,
                    name = "Beatriz",
                    age = 26,
                    bio = "Viajante compulsiva (já visitei 15 países!). Sempre planejando o próximo embarque. Gosto de exposições de arte contemporânea e pôr do sol.",
                    occupation = "Arquiteta",
                    interestTags = "Viagens,Design,Museus,Pôr do Sol",
                    avatarIndex = 5,
                    distanceKm = 2
                ),
                UserProfile(
                    id = 6,
                    name = "Gabriel",
                    age = 28,
                    bio = "Fissurado em corrida de rua, musculação e podcasts sobre astronomia e física quântica. Vamos treinar juntos ou filosofar sobre o espaço?",
                    occupation = "Personal Trainer",
                    interestTags = "Treino,Corrida,Física,Podcasts",
                    avatarIndex = 6,
                    distanceKm = 6
                ),
                UserProfile(
                    id = 7,
                    name = "Juliana",
                    age = 23,
                    bio = "Estudante de veterinária e mãe de 14 plantas. Adoro piqueniques no parque, feirinhas de artesanato locais e sessões intermináveis de cinema cult.",
                    occupation = "Estudante de Med. Veterinária",
                    interestTags = "Animais,Plantas,Cinema,Piquenique",
                    avatarIndex = 7,
                    distanceKm = 4
                ),
                UserProfile(
                    id = 8,
                    name = "Felipe",
                    age = 31,
                    bio = "Fã nato de jazz clássico, vinhos encorpados e culinária italiana. Gosto de cozinhar ouvindo discos de vinil e curtir conversas calmas à noite.",
                    occupation = "Sommelier & Crítico Gastronômico",
                    interestTags = "Vinhos,Massa,Jazz,Discos de Vinil",
                    avatarIndex = 8,
                    distanceKm = 15
                )
            )
            for (p in list) {
                dao.insertProfile(p)
            }
        }
    }
}
