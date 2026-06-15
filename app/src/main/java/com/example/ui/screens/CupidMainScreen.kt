@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ChatMessage
import com.example.data.MatchConversation
import com.example.data.UserProfile
import com.example.ui.CupidViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.components.GradientThemes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Standard brand colors
val CupidPrimary = Color(0xFFFD297B) // Traditional Tinder deep energetic pink
val CupidGradientStart = Color(0xFFFD297B)
val CupidGradientEnd = Color(0xFFFF5864)
val CupidAccent = Color(0xFFFF6B6B)
val GrayBackground = Color(0xFFF7F8FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupidMainScreen(
    viewModel: CupidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUserId = 999L // Matches database initializer ID
    
    // VM States
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle(emptyList())
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle(null)
    val matches by viewModel.allMatches.collectAsStateWithLifecycle(emptyList())
    val unswipedDeck by viewModel.unswipedProfiles.collectAsStateWithLifecycle(emptyList())
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMatchMessages.collectAsStateWithLifecycle(emptyList())
    val celebrationProfile by viewModel.celebrationProfile.collectAsStateWithLifecycle()

    // Screen tab selection (0 = Swipe, 1 = Matches/Chats, 2 = Profile)
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (activeMatch == null) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Descobrir",
                                tint = if (selectedTab == 0) CupidPrimary else Color.Gray,
                                modifier = Modifier.testTag("nav_discover_tab")
                            )
                        },
                        label = {
                            Text(
                                "Descobrir",
                                color = if (selectedTab == 0) CupidPrimary else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (matches.isNotEmpty() && selectedTab != 1) {
                                        Badge(containerColor = CupidPrimary) {
                                            Text(matches.size.toString(), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "Matches",
                                    tint = if (selectedTab == 1) CupidPrimary else Color.Gray,
                                    modifier = Modifier.testTag("nav_matches_tab")
                                )
                            }
                        },
                        label = {
                            Text(
                                "Conversas",
                                color = if (selectedTab == 1) CupidPrimary else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Meu Perfil",
                                tint = if (selectedTab == 2) CupidPrimary else Color.Gray,
                                modifier = Modifier.testTag("nav_profile_tab")
                            )
                        },
                        label = {
                            Text(
                                "Perfil",
                                color = if (selectedTab == 2) CupidPrimary else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GrayBackground)
        ) {
            // Primary tabs routing
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(250, easing = LinearOutSlowInEasing),
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> SwipeTabContent(
                        deck = unswipedDeck,
                        onLike = { profile -> viewModel.handleSwipeRight(profile) },
                        onDislike = { profile -> viewModel.handleSwipeLeft(profile) },
                        onReset = { 
                            viewModel.resetDemo()
                            Toast.makeText(context, "Perfis reiniciados!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> MatchesTabContent(
                        matches = matches,
                        profiles = profiles,
                        activeMatch = activeMatch,
                        activeMessages = activeMessages,
                        currentUserId = currentUserId,
                        onSelectMatch = { viewModel.selectMatch(it) },
                        onCloseChat = { viewModel.closeChat() },
                        onSendMessage = { matchId, txt -> viewModel.sendChatMessage(matchId, txt) }
                    )
                    2 -> ProfileTabContent(
                        currentUser = currentUser,
                        onSaveProfile = { name, age, bio, occ, tags, avatarIdx ->
                            viewModel.saveMyProfile(name, age, bio, occ, tags, avatarIdx)
                            Toast.makeText(context, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                        },
                        onResetAll = {
                            viewModel.resetDemo()
                            Toast.makeText(context, "Todos os dados limpos!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Confetti visual celebration for matches
            celebrationProfile?.let { matchedUser ->
                MatchCelebrateOverlay(
                    matchedUser = matchedUser,
                    onStartChat = {
                        viewModel.dismissCelebration()
                        val matchId = if (currentUserId < matchedUser.id) "match_${currentUserId}_${matchedUser.id}" else "match_${matchedUser.id}_${currentUserId}"
                        val actualMatch = matches.find { it.id == matchId }
                        if (actualMatch != null) {
                            viewModel.selectMatch(actualMatch)
                        } else {
                            viewModel.selectMatch(
                                MatchConversation(id = matchId, user1Id = currentUserId, user2Id = matchedUser.id)
                            )
                        }
                        selectedTab = 1
                    },
                    onDismiss = { viewModel.dismissCelebration() }
                )
            }
        }
    }
}

// ==========================================
// TAB 1: CARD SWIPING TAB
// ==========================================
@Composable
fun SwipeTabContent(
    deck: List<UserProfile>,
    onLike: (UserProfile) -> Unit,
    onDislike: (UserProfile) -> Unit,
    onReset: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App top identity area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Logo",
                    tint = CupidPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cupid Match",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(CupidGradientStart, CupidGradientEnd))
                    )
                )
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(40.dp)
                    .testTag("btn_reset_deck")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reiniciar",
                    tint = Color.DarkGray
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (deck.isEmpty()) {
                // Out of Profiles Empty State
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                        .testTag("empty_deck_view")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🌎",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Ninguém novo por perto!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Você já avaliou todos os perfis disponíveis na sua região. Que tal reiniciar para encontrar novas conexões?",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onReset,
                            colors = ButtonDefaults.buttonColors(containerColor = CupidPrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("btn_reset_deck_empty")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recarregar Perfis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Deck stack
                // Slice top 2 cards for optimal rendering performance & perfect physical stack appearance
                val topCards = deck.take(2).reversed()
                
                topCards.forEachIndexed { idx, profile ->
                    val isTopCard = idx == topCards.lastIndex
                    key(profile.id) {
                        TinderSwipeCard(
                            profile = profile,
                            isTopCard = isTopCard,
                            onLike = { onLike(profile) },
                            onDislike = { onDislike(profile) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TinderSwipeCard(
    profile: UserProfile,
    isTopCard: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag offset states
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Animation transition handles
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }

    // Sync state changes
    LaunchedEffect(offsetX, offsetY) {
        if (!animOffsetX.isRunning && !animOffsetY.isRunning) {
            animOffsetX.snapTo(offsetX)
            animOffsetY.snapTo(offsetY)
        }
    }

    val rotation = (animOffsetX.value / 20f).coerceIn(-15f, 15f)
    val cardScale = if (isTopCard) 1.0f else 0.94f
    val verticalOffset = if (isTopCard) 0.dp else 12.dp

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 6.dp else 2.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
            .offset(y = verticalOffset)
            .scale(cardScale)
            .graphicsLayer {
                translationX = animOffsetX.value
                translationY = animOffsetY.value
                rotationZ = rotation
            }
            .pointerInput(isTopCard) {
                if (!isTopCard) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        val threshX = with(density) { 140.dp.toPx() }
                        coroutineScope.launch {
                            if (offsetX > threshX) {
                                // Fly away right (Liked!)
                                animOffsetX.animateTo(1200f, animationSpec = tween(300))
                                onLike()
                            } else if (offsetX < -threshX) {
                                // Fly away left (Disliked!)
                                animOffsetX.animateTo(-1200f, animationSpec = tween(300))
                                onDislike()
                            } else {
                                // Snap back with nice spring physics
                                launch { animOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                launch { animOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        coroutineScope.launch {
                            animOffsetX.snapTo(offsetX)
                            animOffsetY.snapTo(offsetY)
                        }
                    }
                )
            }
            .testTag("tinder_card_${profile.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Visual top banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(
                        Brush.verticalGradient(
                            colors = GradientThemes.getOrElse(profile.avatarIndex) { GradientThemes[0] }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UserAvatar(
                        avatarIndex = profile.avatarIndex,
                        size = 140.dp,
                        borderWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "A ${profile.distanceKm} km de distância",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dynamic Swipe Stamps
                if (offsetX > 150f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(24.dp)
                            .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("LIKE", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                } else if (offsetX < -150f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .border(3.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("NOPE", color = Color(0xFFFF5252), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Description bottom section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.age.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.DarkGray
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBox,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.occupation,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = profile.bio,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Interest chips row
                Text(
                    "Interesses",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    profile.interestTags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF0F1F5), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                color = Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rapid circular buttons
                if (isTopCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    animOffsetX.animateTo(-1200f, tween(300))
                                    onDislike()
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFFFF0F0), CircleShape)
                                .border(1.dp, Color(0xFFFFD4D4), CircleShape)
                                .size(54.dp)
                                .testTag("btn_swipe_nope")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Nope", tint = Color(0xFFFF4949), modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    // Pulse / scale visual jump then trigger right
                                    animOffsetX.animateTo(1200f, tween(300))
                                    onLike()
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFFFF0F5), CircleShape)
                                .border(1.dp, Color(0xFFFFC0D3), CircleShape)
                                .size(54.dp)
                                .testTag("btn_swipe_like")
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = CupidPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: MATCHES & CHATS TAB
// ==========================================
@Composable
fun MatchesTabContent(
    matches: List<MatchConversation>,
    profiles: List<UserProfile>,
    activeMatch: MatchConversation?,
    activeMessages: List<ChatMessage>,
    currentUserId: Long,
    onSelectMatch: (MatchConversation) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String, String) -> Unit
) {
    AnimatedContent(
        targetState = activeMatch,
        transitionSpec = {
            if (targetState != null) {
                // Slide in chat screen
                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                // Slide back to lists
                slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "ChatScreenNavigator"
    ) { currentActiveMatch ->
        if (currentActiveMatch != null) {
            // Active chat view
            val partnerId = if (currentActiveMatch.user1Id == currentUserId) currentActiveMatch.user2Id else currentActiveMatch.user1Id
            val partnerProfile = profiles.find { it.id == partnerId }
            
            ChatRoomScreen(
                match = currentActiveMatch,
                messages = activeMessages,
                partnerProfile = partnerProfile,
                currentUserId = currentUserId,
                onBack = onCloseChat,
                onSendMessage = { msg -> onSendMessage(currentActiveMatch.id, msg) }
            )
        } else {
            // Conversas / matches listed
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Minhas Conversas",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (matches.isEmpty()) {
                    // Empty state matching
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(bottom = 12.dp)
                            )
                            Text(
                                "Nenhuma conversa ainda",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Vá para a aba Descobrir e curta alguns perfis! Quando eles corresponderem de volta, você verá suas conversas por aqui.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matches) { match ->
                            val partnerId = if (match.user1Id == currentUserId) match.user2Id else match.user1Id
                            val partnerProfile = profiles.find { it.id == partnerId }
                            
                            if (partnerProfile != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectMatch(match) }
                                        .testTag("match_item_${partnerProfile.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            UserAvatar(avatarIndex = partnerProfile.avatarIndex, size = 56.dp)
                                            // Active/Online green dot
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .background(Color(0xFF4CAF50), CircleShape)
                                                    .border(2.dp, Color.White, CircleShape)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = partnerProfile.name,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                                Text(
                                                    "Agora",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Clique aqui para bater papo com ${partnerProfile.name}...",
                                                fontSize = 13.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    match: MatchConversation,
    messages: List<ChatMessage>,
    partnerProfile: UserProfile?,
    currentUserId: Long,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll down on layout
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // App bar
        TopAppBar(
            title = {
                if (partnerProfile != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(avatarIndex = partnerProfile.avatarIndex, size = 40.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(partnerProfile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online agora", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    Text("Chat")
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_close_chat")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Divider
        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

        // Scrolling lists
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF9F9FA))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUserId
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isMe) CupidPrimary else Color(0xFFE8E9ED),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .widthIn(max = 260.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = if (isMe) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Agora",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        // Quick tapping emojis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F1F3))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("👋 Oi!", "❤️", "😍", "😂", "Vem me ver!", "Tudo ótimo!").forEach { emojiTxt ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable { onSendMessage(emojiTxt) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(emojiTxt, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                }
            }
        }

        // Keyboard footer area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = { Text("Mande uma mensagem...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F1F3),
                    unfocusedContainerColor = Color(0xFFF1F1F3),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onSendMessage(textValue)
                        textValue = ""
                    }
                },
                modifier = Modifier
                    .background(CupidPrimary, CircleShape)
                    .size(48.dp)
                    .testTag("btn_send_msg")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// TAB 3: PROFILE TAB EDITOR
// ==========================================
@Composable
fun ProfileTabContent(
    currentUser: UserProfile?,
    onSaveProfile: (String, Int, String, String, String, Int) -> Unit,
    onResetAll: () -> Unit
) {
    if (currentUser == null) return

    // Fields inside states
    var name by remember(currentUser.name) { mutableStateOf(currentUser.name) }
    var ageScale by remember(currentUser.age) { mutableStateOf(currentUser.age.toFloat()) }
    var bio by remember(currentUser.bio) { mutableStateOf(currentUser.bio) }
    var occupation by remember(currentUser.occupation) { mutableStateOf(currentUser.occupation) }
    var selectedAvatarIndex by remember(currentUser.avatarIndex) { mutableStateOf(currentUser.avatarIndex) }
    
    // Split user tags and parse into active sets
    val initialTags = currentUser.interestTags.split(",").filter { it.isNotBlank() }.toSet()
    val activeTagsSet = remember(currentUser.interestTags) { mutableStateMapOf<String, Boolean>() }
    
    val allPreselectedTags = listOf(
        "Tecnologia", "Café", "Música", "Cinema", "Trilha", "Arte", 
        "Sushi", "Viagens", "Praia", "Cachorros", "Escalada", 
        "Cozinha", "Rock", "Animes", "Pôr do Sol"
    )

    // Sync state
    LaunchedEffect(currentUser.interestTags) {
        activeTagsSet.clear()
        allPreselectedTags.forEach { tag ->
            activeTagsSet[tag] = initialTags.contains(tag)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Meu Perfil Cupid",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Text(
                "Configure como você aparece para novas pessoas na plataforma.",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        // Live Preview of user card style
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        avatarIndex = selectedAvatarIndex,
                        size = 72.dp,
                        borderWidth = 3.dp,
                        borderColor = CupidPrimary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "$name, ${ageScale.roundToInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = occupation.ifBlank { "Nenhum cargo adicionado" },
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bio.ifBlank { "Nenhuma bio cadastrada." },
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Text field name
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome de exibição") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CupidPrimary,
                    focusedLabelColor = CupidPrimary
                )
            )
        }

        // Age slider
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Minha idade", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    Text("${ageScale.roundToInt()} anos", color = CupidPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = ageScale,
                    onValueChange = { ageScale = it },
                    valueRange = 18f..65f,
                    colors = SliderDefaults.colors(
                        thumbColor = CupidPrimary,
                        activeTrackColor = CupidPrimary
                    ),
                    modifier = Modifier.testTag("profile_age_slider")
                )
            }
        }

        // Occupation Fields
        item {
            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("Profissão / Estudo") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CupidPrimary,
                    focusedLabelColor = CupidPrimary
                )
            )
        }

        // Biography Text editor
        item {
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Escreva uma biografia encantadora") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CupidPrimary,
                    focusedLabelColor = CupidPrimary
                )
            )
        }

        // Stylized Avatar selection
        item {
            Text(
                "Escolha o seu Avatar / Tema",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0..8).forEach { idx ->
                    Box(
                        modifier = Modifier
                            .clickable { selectedAvatarIndex = idx }
                            .padding(2.dp)
                    ) {
                        UserAvatar(
                            avatarIndex = idx,
                            size = 46.dp,
                            borderWidth = 3.dp,
                            borderColor = if (selectedAvatarIndex == idx) CupidPrimary else Color.Transparent
                        )
                    }
                }
            }
        }

        // Checklist of tags for user interests
        item {
            Text(
                "Hobbies & Interesses",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allPreselectedTags.forEach { tag ->
                    val isChecked = activeTagsSet[tag] ?: false
                    Box(
                        modifier = Modifier
                            .background(
                                if (isChecked) CupidPrimary else Color(0xFFECEFF1),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                activeTagsSet[tag] = !isChecked
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (isChecked) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Command Button row
        item {
            Button(
                onClick = {
                    val finalTagsString = activeTagsSet.filter { it.value }.keys.joinToString(",")
                    onSaveProfile(
                        name,
                        ageScale.roundToInt(),
                        bio,
                        occupation,
                        finalTagsString,
                        selectedAvatarIndex
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_profile"),
                colors = ButtonDefaults.buttonColors(containerColor = CupidPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Perfil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Heavy resetting action
        item {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpar Fotos & Swipes (Reset)", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// MATCH CELEBRATION DUST COVER OVERLAY
// ==========================================
@Composable
fun MatchCelebrateOverlay(
    matchedUser: UserProfile,
    onStartChat: () -> Unit,
    onDismiss: () -> Unit
) {
    // Beautiful bounce scaling for romantic hearts
    val infiniteTransition = rememberInfiniteTransition(label = "romantic_heart")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block standard touches from flowing underlying */ }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Deu Match! ❤️",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFF4081), Color(0xFFFF80AB)))
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Você e ${matchedUser.name} curtiram um ao outro!",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Joined Avatars display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left avatar representing currentUser (You, index 0 default)
                UserAvatar(
                    avatarIndex = 0,
                    size = 110.dp,
                    borderWidth = 4.dp,
                    borderColor = Color.White
                )

                // Intersecting Heart Centerpiece
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Coráculo",
                    tint = CupidPrimary,
                    modifier = Modifier
                        .size(60.dp)
                        .scale(pulseScale)
                        .padding(horizontal = 8.dp)
                )

                // Right avatar representing matched user
                UserAvatar(
                    avatarIndex = matchedUser.avatarIndex,
                    size = 110.dp,
                    borderWidth = 4.dp,
                    borderColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Chat action
            Button(
                onClick = onStartChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_matching_chat"),
                colors = ButtonDefaults.buttonColors(containerColor = CupidPrimary),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enviar Mensagem Agora",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dismiss navigate
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(
                    text = "Continuar Navegando",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
