package com.fitlog.app
import com.fitlog.app.data.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class StylingTest {
    private val items=listOf(Garment(1,"蓝衬衫","上衣",mainColor="#A9BEC7"),Garment(2,"灰长裤","裤子",mainColor="#555C5B"),Garment(3,"帆布鞋","鞋履",mainColor="#E8E5DA"),Garment(4,"牛仔裤","裤子",mainColor="#4E6579"),Garment(5,"内裤","内裤",mainColor="#292D2C"))
    @Test fun anchorAndRealItemsArePreserved(){val results=Styling.suggest(items,1);assertTrue(results.size>1);results.forEach{assertTrue(it.ids.contains(1L));assertEquals(it.ids.size,it.ids.distinct().size);assertFalse(it.ids.contains(5L));assertTrue(it.ids.all{id->items.any{it.id==id}});assertEquals(listOf(Styling.Slot.TOP,Styling.Slot.BOTTOM,Styling.Slot.SHOES),it.ids.map{id->Styling.slot(items.first{it.id==id}.type)})}}
    @Test fun missingGarmentsAreNotInvented(){val result=Styling.suggest(items.take(1),1).single();assertEquals(listOf(1L),result.ids);assertEquals(listOf(Styling.Slot.BOTTOM,Styling.Slot.SHOES),result.missing);assertTrue(Styling.suggest(items,5).isEmpty())}
    @Test fun unknownColorsRemainUnknown(){assertNull(Styling.hex("深蓝"));assertTrue(Styling.relation(items[0].copy(mainColor=""),items[1]).reason.contains("尚未标注"))}
    @Test fun paletteDoesNotMatchEveryMutedColor(){val palette=listOf("#A9BEC7","#555C5B","#E8E5DA");assertTrue(Styling.matchesPalette("#A9BEC7",palette));assertFalse(Styling.matchesPalette("#788674",palette));assertFalse(Styling.matchesPalette("#C4B9A4",palette))}
    @Test fun measurementBasisAndUnitsMustAgree(){val a=GarmentAttr(name="胸围",value="106",basis="周长");val b=GarmentAttr(name="胸围",value="1100",unit="mm",basis="周长");assertEquals(4.0,MeasurementComparison.rows(listOf(a),listOf(b)).single().deltaCm!!,.001);assertNull(MeasurementComparison.rows(listOf(a),listOf(b.copy(basis="平铺单面"))).single().deltaCm);assertNull(MeasurementComparison.rows(listOf(a.copy(basis="")),listOf(b)).single().deltaCm)}
    @Test fun invalidNumbersCannotCreateFalsePrecision(){val a=GarmentAttr(name="腰围",value="80",basis="周长");listOf("NaN","Infinity","-1","未知").forEach{assertNull(MeasurementComparison.rows(listOf(a),listOf(a.copy(value=it))).single().deltaCm)}}
    @Test fun oldBackupRemainsReadable(){val b=Json.decodeFromString<BackupData>("""{"version":1,"garments":[{"id":1,"name":"旧衬衫","type":"上衣","fit":"合身"}],"attrs":[{"garmentId":1,"name":"胸围","value":"106"}]}""");assertEquals("旧衬衫",b.garments.single().name);assertEquals("",b.garments.single().mainColor);assertEquals("",b.attrs.single().basis);assertTrue(b.outfits.isEmpty())}
}
