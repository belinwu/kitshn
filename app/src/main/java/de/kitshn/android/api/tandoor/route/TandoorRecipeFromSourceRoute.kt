package de.kitshn.android.api.tandoor.route

import de.kitshn.android.api.tandoor.TandoorClient
import de.kitshn.android.api.tandoor.TandoorRequestsError
import de.kitshn.android.api.tandoor.model.recipe.TandoorRecipeFromSource
import de.kitshn.android.api.tandoor.postObject
import org.json.JSONObject

class TandoorRecipeFromSourceRoute(client: TandoorClient) : TandoorBaseRoute(client) {

    @Throws(TandoorRequestsError::class)
    suspend fun fetch(url: String): TandoorRecipeFromSource {
        val data = JSONObject().apply {
            put("url", url)
            put("data", "")
        }

        return TandoorRecipeFromSource.parse(
            client,
            client.postObject("/recipe-from-source/", data).toString()
        )
    }

}