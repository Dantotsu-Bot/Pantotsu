package ani.dantotsu.settings.saving

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import ani.dantotsu.settings.saving.internal.Compat
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.snackString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object PrefManager {

    private var generalPreferences: SharedPreferences? = null
    private var animePreferences: SharedPreferences? = null
    private var mangaPreferences: SharedPreferences? = null
    private var playerPreferences: SharedPreferences? = null
    private var readerPreferences: SharedPreferences? = null
    private var irrelevantPreferences: SharedPreferences? = null
    private var animeDownloadsPreferences: SharedPreferences? = null
    private var protectedPreferences: SharedPreferences? = null

    fun init(context: Context) {  //must be called in Application class or will crash
        generalPreferences = context.getSharedPreferences(Location.General.location, Context.MODE_PRIVATE)
        animePreferences = context.getSharedPreferences(Location.Anime.location, Context.MODE_PRIVATE)
        mangaPreferences = context.getSharedPreferences(Location.Manga.location, Context.MODE_PRIVATE)
        playerPreferences = context.getSharedPreferences(Location.Player.location, Context.MODE_PRIVATE)
        readerPreferences = context.getSharedPreferences(Location.Reader.location, Context.MODE_PRIVATE)
        irrelevantPreferences = context.getSharedPreferences(Location.Irrelevant.location, Context.MODE_PRIVATE)
        animeDownloadsPreferences = context.getSharedPreferences(Location.AnimeDownloads.location, Context.MODE_PRIVATE)
        protectedPreferences = context.getSharedPreferences(Location.Protected.location, Context.MODE_PRIVATE)
        Compat.importOldPrefs(context)
    }

    fun <T> setVal(prefName: PrefName, value: T?) {
        val pref = getPrefLocation(prefName.data.prefLocation)
        with(pref.edit()) {
            when (value) {
                is Boolean -> putBoolean(prefName.name, value)
                is Int -> putInt(prefName.name, value)
                is Float -> putFloat(prefName.name, value)
                is Long -> putLong(prefName.name, value)
                is String -> putString(prefName.name, value)
                is Set<*> -> convertAndPutStringSet(prefName.name, value)
                null -> remove(prefName.name)
                else -> serialzeClass(prefName.name, value)
            }
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getVal(prefName: PrefName, default: T) : T {
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(prefName.name, default as Boolean) as T
                Int::class -> pref.getInt(prefName.name, default as Int) as T
                Float::class -> pref.getFloat(prefName.name, default as Float) as T
                Long::class -> pref.getLong(prefName.name, default as Long) as T
                String::class -> pref.getString(prefName.name, default as String?) as T
                Set::class -> convertFromStringSet(pref.getStringSet(prefName.name, null), default) as T
                List::class -> deserialzeClass(prefName.name, default) as T
                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getVal(prefName: PrefName) : T {
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(prefName.name, prefName.data.default as Boolean) as T
                Int::class -> pref.getInt(prefName.name, prefName.data.default as Int) as T
                Float::class -> pref.getFloat(prefName.name, prefName.data.default as Float) as T
                Long::class -> pref.getLong(prefName.name, prefName.data.default as Long) as T
                String::class -> pref.getString(prefName.name, prefName.data.default as String?) as T
                Set::class -> convertFromStringSet(pref.getStringSet(prefName.name, null), prefName.data.default) as T
                List::class -> deserialzeClass(prefName.name, prefName.data.default) as T
                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            prefName.data.default as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getNullableVal(prefName: PrefName, default: T?) : T? {
        return try {
            val pref = getPrefLocation(prefName.data.prefLocation)
            when (prefName.data.type) {
                Boolean::class -> pref.getBoolean(prefName.name, prefName.data.default as Boolean) as T?
                Int::class -> pref.getInt(prefName.name, prefName.data.default as Int) as T?
                Float::class -> pref.getFloat(prefName.name, prefName.data.default as Float) as T?
                Long::class -> pref.getLong(prefName.name, prefName.data.default as Long) as T?
                String::class -> pref.getString(prefName.name, prefName.data.default as String?) as T?
                Set::class -> convertFromStringSet(pref.getStringSet(prefName.name, null), prefName.data.default) as T?
                else -> deserialzeClass(prefName.name, default)
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCustomVal(key: String, default: T): T {
        return try {
            when (default) {
                is Boolean -> irrelevantPreferences!!.getBoolean(key, default) as T
                is Int -> irrelevantPreferences!!.getInt(key, default) as T
                is Float -> irrelevantPreferences!!.getFloat(key, default) as T
                is Long -> irrelevantPreferences!!.getLong(key, default) as T
                is String -> irrelevantPreferences!!.getString(key, default) as T
                is Set<*> -> convertFromStringSet(irrelevantPreferences!!.getStringSet(key, null), default) as T
                else -> throw IllegalArgumentException("Type not supported")
            }
        } catch (e: Exception) {
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getNullableCustomVal(key: String, default: T): T? {
        return try {
            when (default) {
                is Boolean -> irrelevantPreferences!!.getBoolean(key, default) as T?
                is Int -> irrelevantPreferences!!.getInt(key, default) as T?
                is Float -> irrelevantPreferences!!.getFloat(key, default) as T?
                is Long -> irrelevantPreferences!!.getLong(key, default) as T?
                is String -> irrelevantPreferences!!.getString(key, default) as T?
                is Set<*> -> convertFromStringSet(irrelevantPreferences!!.getStringSet(key, null), default) as T?
                else -> deserialzeClass(key, default)
            }
        } catch (e: Exception) {
            default
        }
    }

    fun removeVal(prefName: PrefName) {
        val pref = getPrefLocation(prefName.data.prefLocation)
        with(pref.edit()) {
            remove(prefName.name)
            apply()
        }
    }

    fun <T> setCustomVal(key: String, value: T?) {
        //for custom force irrelevant
        with(irrelevantPreferences!!.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value as Boolean)
                is Int -> putInt(key, value as Int)
                is Float -> putFloat(key, value as Float)
                is Long -> putLong(key, value as Long)
                is String -> putString(key, value as String)
                is Set<*> -> convertAndPutStringSet(key, value)
                null -> remove(key)
                else -> serialzeClass(key, value)
            }
            apply()
        }
    }

    fun removeCustomVal(key: String) {
        //for custom force irrelevant
        with(irrelevantPreferences!!.edit()) {
            remove(key)
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getLiveVal(prefName: PrefName, default: T) : SharedPreferenceLiveData<T> {
        val pref = getPrefLocation(prefName.data.prefLocation)
        return when (prefName.data.type) {
            Boolean::class -> SharedPreferenceBooleanLiveData(
                pref,
                prefName.name,
                default as Boolean
            ) as SharedPreferenceLiveData<T>
            Int::class -> SharedPreferenceIntLiveData(
                pref,
                prefName.name,
                default as Int
            ) as SharedPreferenceLiveData<T>
            Float::class -> SharedPreferenceFloatLiveData(
                pref,
                prefName.name,
                default as Float
            ) as SharedPreferenceLiveData<T>
            Long::class -> SharedPreferenceLongLiveData(
                pref,
                prefName.name,
                default as Long
            ) as SharedPreferenceLiveData<T>
            String::class -> SharedPreferenceStringLiveData(
                pref,
                prefName.name,
                default as String
            ) as SharedPreferenceLiveData<T>
            Set::class -> SharedPreferenceStringSetLiveData(
                pref,
                prefName.name,
                default as Set<String>
            ) as SharedPreferenceLiveData<T>
            else -> throw IllegalArgumentException("Type not supported")
        }
    }

    fun SharedPreferenceLiveData<*>.asLiveBool(): SharedPreferenceBooleanLiveData =
        this as? SharedPreferenceBooleanLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Boolean>")

    fun SharedPreferenceLiveData<*>.asLiveInt(): SharedPreferenceIntLiveData =
        this as? SharedPreferenceIntLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Int>")

    fun SharedPreferenceLiveData<*>.asLiveFloat(): SharedPreferenceFloatLiveData =
        this as? SharedPreferenceFloatLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Float>")

    fun SharedPreferenceLiveData<*>.asLiveLong(): SharedPreferenceLongLiveData =
        this as? SharedPreferenceLongLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Long>")

    fun SharedPreferenceLiveData<*>.asLiveString(): SharedPreferenceStringLiveData =
        this as? SharedPreferenceStringLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<String>")

    fun SharedPreferenceLiveData<*>.asLiveStringSet(): SharedPreferenceStringSetLiveData =
        this as? SharedPreferenceStringSetLiveData
            ?: throw ClassCastException("Cannot cast to SharedPreferenceLiveData<Set<String>>")

    fun getAnimeDownloadPreferences(): SharedPreferences = animeDownloadsPreferences!!  //needs to be used externally

    fun exportAllPrefs(prefLocation: Location): Map<String, *>{
        val pref = getPrefLocation(prefLocation)
        val typedMap = mutableMapOf<String, Any>()
        pref.all.forEach { (key, value) ->
            val typeValueMap = mapOf(
                "type" to value?.javaClass?.kotlin?.qualifiedName,
                "value" to value
            )
            typedMap[key] = typeValueMap
        }

        return typedMap
    }

    @Suppress("UNCHECKED_CAST")
    fun importAllPrefs(prefs: Map<String, *>, prefLocation: Location) {
        val pref = getPrefLocation(prefLocation)
        with(pref.edit()) {
            prefs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is HashSet<*> -> putStringSet(key, value as Set<String>)
                    is ArrayList<*> -> putStringSet(key, arrayListToSet(value))
                    is Set<*> -> putStringSet(key, value as Set<String>)
                    else -> snackString("Error importing preference: Type not supported")
                }
            }
            apply()
        }
    }

    private fun arrayListToSet(arrayList: ArrayList<*>): Set<String> {
        return arrayList.map { it.toString() }.toSet()
    }

    private fun getPrefLocation(prefLoc: Location): SharedPreferences {
        return when (prefLoc) {
            Location.General -> generalPreferences
            Location.UI -> generalPreferences
            Location.Anime -> animePreferences
            Location.Manga -> mangaPreferences
            Location.Player -> playerPreferences
            Location.Reader -> readerPreferences
            Location.NovelReader -> readerPreferences
            Location.Irrelevant -> irrelevantPreferences
            Location.AnimeDownloads -> animeDownloadsPreferences
            Location.Protected -> protectedPreferences
        }!!
    }

    private fun <T> convertFromStringSet(stringSet: Set<String>?, default: T): Set<*> {
        if (stringSet.isNullOrEmpty()) return default as Set<*>

        return try {
            val typeIdentifier = stringSet.first()
            val convertedSet = stringSet.drop(1) // Remove the type identifier
            when (typeIdentifier) {
                "Int" -> convertedSet.mapNotNull { it.toIntOrNull() }.toSet()
                "Boolean" -> convertedSet.mapNotNull { it.toBooleanStrictOrNull() }.toSet()
                "Float" -> convertedSet.mapNotNull { it.toFloatOrNull() }.toSet()
                "Long" -> convertedSet.mapNotNull { it.toLongOrNull() }.toSet()
                "String" -> convertedSet.toSet()
                else -> stringSet
            }
        } catch (e: Exception) {
            snackString("Error converting preference: ${e.message}")
            default as Set<*>
        }
    }

    private fun SharedPreferences.Editor.convertAndPutStringSet(key: String, value: Set<*>) {
        val typeIdentifier = when (value.firstOrNull()) {
            is Int -> "Int"
            is Boolean -> "Boolean"
            is Float -> "Float"
            is Long -> "Long"
            is String -> "String"
            null -> return
            else -> throw IllegalArgumentException("Type not supported")
        }
        val stringSet = setOf(typeIdentifier) + value.map { it.toString() }
        putStringSet(key, stringSet)
    }


    private fun <T> serialzeClass(key: String, value: T){
        try {
            val bos = ByteArrayOutputStream()
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(value)
            }

            val serialized = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT)
            irrelevantPreferences!!.edit().putString(key, serialized).apply()
        } catch (e: Exception) {
            snackString("Error serializing preference: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserialzeClass(key: String, default: T?): T? {
        return try {
            val serialized = irrelevantPreferences!!.getString(key, null)
            if (serialized != null) {
                val data = Base64.decode(serialized, Base64.DEFAULT)
                val bis = ByteArrayInputStream(data)
                val ois = ObjectInputStream(bis)
                val obj = ois.readObject() as T?
                obj
            } else {
                default
            }
        } catch (e: Exception) {
            snackString("Error deserializing preference: ${e.message}")
            default
        }
    }
}