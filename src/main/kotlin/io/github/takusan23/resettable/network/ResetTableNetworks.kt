package io.github.takusan23.resettable.network

import net.minecraft.util.Identifier

/**
 * network
 *
 * クライアント側 (Screenなど) -> サーバー で値を渡すために network を使う。
 *
 * クライアント側の変更はサーバー側には反映されていないので
 * */
object ResetTableNetworks {

    /**
     * リセットテーブルのページ切り替えをサーバーへ通知するための識別子
     * */
    val SEND_RECIPE_INDEX_CHANGE = Identifier("resettable", "recipe_page_index")

}