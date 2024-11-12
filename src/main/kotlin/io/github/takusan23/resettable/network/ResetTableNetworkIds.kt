package io.github.takusan23.resettable.network

import net.minecraft.util.Identifier

/** ネットワークの ID 一覧 */
object ResetTableNetworkIds {

    /** リセットテーブルで戻せない理由をサーバーからクライアントに送るためのネットワークの ID */
    val RESET_TABLE_ERROR_NETWORK_ID = Identifier.of("clickmanaita", "reset_table_error_network")

}