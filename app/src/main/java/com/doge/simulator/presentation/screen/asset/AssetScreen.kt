package com.doge.simulator.presentation.screen.asset

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.presentation.component.InfoDialog
import com.doge.simulator.presentation.component.InfoEntry
import com.doge.simulator.presentation.component.SettingsDialog
import com.doge.simulator.presentation.component.rememberLiveCoinDisplay
import com.doge.simulator.presentation.viewmodel.AssetViewModel
import com.doge.simulator.presentation.viewmodel.AuthViewModel
import com.doge.simulator.presentation.viewmodel.SettingsViewModel
import com.doge.simulator.ui.theme.*

@Composable
fun AssetScreen(
    onRankClick: () -> Unit = {},
    onSignedOut: () -> Unit = {},
    viewModel: AssetViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val displayCoins = rememberLiveCoinDisplay(baseCoins = state.coins, netPerMin = state.netProductionPerMin)
    val displayTotalAsset = displayCoins + state.totalMarketValue
    val message by viewModel.message.collectAsState()
    val resources = state.resources
    var sellDialogResource by remember { mutableStateOf<Resource?>(null) }
    var showAssetInfo by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var isSigningOut by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val bgmEnabled by settingsViewModel.bgmEnabled.collectAsState()
    val authState by authViewModel.state.collectAsState()

    if (showSettings) {
        SettingsDialog(
            uid = settingsViewModel.uid,
            bgmEnabled = bgmEnabled,
            onBgmChange = settingsViewModel::setBgmEnabled,
            onSignOutClick = {
                showSettings = false
                showSignOutConfirm = true
            },
            onDismiss = { showSettings = false }
        )
    }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Unauthenticated) onSignedOut()
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("로그아웃") },
            text = { Text("로그아웃하면 로그인 화면으로 돌아가요. 다시 로그인하면 이어서 할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    isSigningOut = true
                    authViewModel.signOut()
                }) { Text("로그아웃", color = GoldAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("취소") }
            },
            containerColor = SpaceNavy,
            shape = RoundedCornerShape(16.dp),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.lg, bottom = Spacing.xxl)
    ) {
        // ── 상태 메시지 ────────────────────────────────────────────────
        message?.let {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                color = SpaceBlue.copy(alpha = 0.3f)) {
                Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        // ── 헤더 ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "나의 자산",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "보유 자산과 자원 현황을 확인하세요",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                enabled = !isSigningOut,
                onClick = { showSettings = true },
                contentPadding = ButtonPadding.listItemAction,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SpaceMid),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("설정", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── 총 자산 히어로 카드 ────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .textured(shape = RoundedCornerShape(16.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "총 자산 (DGT)",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "%,.2f".format(displayTotalAsset.toDouble()),
                            color = GoldAccent,
                            style = NumericXLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (state.netProductionPerMin > 0L) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_energy),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = "+%,d".format(state.netProductionPerMin),
                                    color = StatusGreen,
                                    style = NumericSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "/ 분",
                                    color = StatusGreen.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── 보유 자원 섹션 ─────────────────────────────────────────────
        SectionHeader(title = "보유 자원")
        Spacer(modifier = Modifier.height(Spacing.md))

        if (resources.isEmpty()) {
            Surface(modifier = Modifier
                .fillMaxWidth()
                .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)), shape = RoundedCornerShape(12.dp),
                color = Color.Transparent, border = BorderStroke(1.dp, SpaceMid)) {
                Text("탐사를 통해 자원을 획득하세요", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = androidx.compose.ui.Modifier.padding(Spacing.lg))
            }
        } else {
            Card(modifier = Modifier
                .fillMaxWidth()
                .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, SpaceMid)) {
                Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                    resources.forEachIndexed { index, resource ->
                        ResourceRow(
                            iconRes = resource.type.iconRes,
                            name = resource.type.displayName,
                            amount = "%,d".format(resource.amount),
                            changePercent = null,
                            changePositive = true,
                            onSell = { sellDialogResource = resource }
                        )
                        if (index < resources.size - 1) {
                            HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = Spacing.lg))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── 보유 자산 섹션 ─────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(title = "보유 자산")
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "자산 항목 설명",
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { showAssetInfo = true }
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, SpaceMid)
        ) {
            Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                ResourceRow(
                    iconRes = R.drawable.ic_ui_coin,
                    name = "DGT",
                    amount = "%,.2f".format(displayCoins.toDouble()),
                    changePercent = if (state.netProductionPerMin > 0L)
                        "+%,d / 분".format(state.netProductionPerMin) else null,
                    changePositive = true
                )
                HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = Spacing.lg))
                ResourceRow(
                    iconRes = R.drawable.ic_ui_usdt,
                    name = "USDT",
                    amount = "%,.2f".format(state.totalMarketValue.toDouble()),
                    changePercent = null,
                    changePositive = true
                )
                HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = Spacing.lg))
                ResourceRow(
                    iconRes = R.drawable.ic_ui_nft_asset,
                    name = "행성 NFT",
                    amount = "${state.planetCount}",
                    changePercent = null,
                    changePositive = true,
                    unit = "개"
                )

            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // ── 요약 스탯 (생산 관련 파생 스탯 — 보유 자산 카드와 중복 없이) ─────
        val netPerMin = state.netProductionPerMin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_ui_stat_profit,
                label = "누적 순수익",
                value = "%,d".format(state.totalProfit),
                unit = "코인",
                valueColor = StatusGreen
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_ui_energy,
                label = "분당 순생산",
                value = (if (netPerMin > 0L) "+" else "") + "%,d".format(netPerMin),
                unit = "코인 / 분",
                valueColor = when {
                    netPerMin > 0L -> StatusGreen
                    netPerMin < 0L -> StatusRed
                    else -> TextPrimary
                }
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        OutlinedButton(
            onClick = onRankClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceAccent),
            border = BorderStroke(1.dp, SpaceAccent.copy(alpha = 0.5f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Image(
                    painter = painterResource(R.drawable.ic_ui_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(IconGlyphSize.small.value.dp)
                )
                Text(text = "랭킹 보기", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (isSigningOut) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldAccent)
        }
    }
    }

    sellDialogResource?.let { resource ->
        SellResourceDialog(
            resource = resource,
            onConfirm = { quantity ->
                viewModel.sellResource(resource.type, quantity)
                sellDialogResource = null
            },
            onDismiss = { sellDialogResource = null }
        )
    }

    if (showAssetInfo) {
        InfoDialog(title = "자산 항목 안내", onDismiss = { showAssetInfo = false }) {
            InfoEntry(
                "DGT",
                "게임의 기본 화폐(코인)예요. 행성 방치 수익과 탐사 보상으로 늘어나고, 강화·구매·고용에 쓰여요."
            )
            InfoEntry(
                "USDT",
                "보유한 행성 전체의 현재 매도 가치예요. 행성을 팔면 그만큼 DGT로 바뀌어요."
            )
            InfoEntry(
                "행성 NFT",
                "지금 보유 중인 행성 개수예요."
            )
            Text(
                "맨 위 '총 자산(DGT)'은 DGT와 행성 시세를 더한 값이에요. 랭킹도 이 기준이에요.",
                color = TextSecondary,
                style = BodyReading
            )
        }
    }
}

@Composable
private fun SellResourceDialog(
    resource: Resource,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val unitPrice = GameConstants.RESOURCE_SELL_PRICE[resource.type] ?: 10L
    var quantityText by remember(resource.type) { mutableStateOf(resource.amount.toString()) }
    val quantity = quantityText.toLongOrNull() ?: 0L
    val isValid = quantity in 1..resource.amount
    val totalCoins = unitPrice * quantity.coerceAtLeast(0L)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            border = BorderStroke(1.dp, SpaceMid),
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(Spacing.xxl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(resource.type.iconRes),
                        contentDescription = resource.type.displayName,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        resource.type.displayName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    "보유 수량: ${"%,d".format(resource.amount)}개 · 단가 ${"%,d".format(unitPrice)}코인/개",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input -> if (input.length <= 12 && input.all { it.isDigit() }) quantityText = input },
                    label = { Text("판매 수량") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { quantityText = resource.amount.toString() }) {
                            Text("전체", color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    isError = quantityText.isNotEmpty() && !isValid
                )

                if (quantityText.isNotEmpty() && !isValid) {
                    Text(
                        if (quantity > resource.amount) "보유 수량을 초과했습니다" else "1개 이상 입력하세요",
                        color = StatusRed,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("예상 판매 금액", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            "+${"%,d".format(totalCoins)} 코인",
                            color = GoldAccent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpaceMid),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("취소") }
                    Button(
                        onClick = { onConfirm(quantity) },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                        border = ButtonDepth.highlightBorder,
                        elevation = ButtonDepth.elevation()
                    ) { Text("판매", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ResourceRow(
    name: String,
    amount: String,
    changePercent: String?,
    changePositive: Boolean,
    unit: String = "",
    @DrawableRes iconRes: Int,
    onSell: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(iconRes),
            contentDescription = name,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amount $unit",
                color = GoldAccent,
                style = NumericSmall,
                fontWeight = FontWeight.SemiBold
            )
            changePercent?.let {
                Text(
                    text = it,
                    color = if (changePositive) StatusGreen else StatusRed,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        if (onSell != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            OutlinedButton(
                onClick = onSell,
                contentPadding = ButtonPadding.listItemAction,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
            ) {
                Text("판매", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = TextPrimary
) {
    Card(
        modifier = modifier.textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceMid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(text = value, color = valueColor, style = NumericSmall, fontWeight = FontWeight.Bold)
            Text(text = unit, color = valueColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

