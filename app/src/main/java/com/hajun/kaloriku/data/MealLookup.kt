package com.hajun.kaloriku.data

/** Mencari satu catatan berdasarkan [entryId]. Mengembalikan null jika tidak ada. */
fun List<MealEntry>.findEntry(entryId: Long): MealEntry? = firstOrNull { it.id == entryId }
