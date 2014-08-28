package com.github.zxh.classpy.dexfile;

import com.github.zxh.classpy.dexfile.body.ClassDefItem;
import com.github.zxh.classpy.dexfile.datatype.UInt;
import com.github.zxh.classpy.dexfile.data.ClassDataItem;
import com.github.zxh.classpy.dexfile.data.MapItem;
import com.github.zxh.classpy.dexfile.data.StringDataItem;
import com.github.zxh.classpy.dexfile.data.TypeItem;
import com.github.zxh.classpy.dexfile.header.HeaderItem;
import com.github.zxh.classpy.dexfile.body.ids.FieldIdItem;
import com.github.zxh.classpy.dexfile.body.ids.MethodIdItem;
import com.github.zxh.classpy.dexfile.body.ids.ProtoIdItem;
import com.github.zxh.classpy.dexfile.body.ids.StringIdItem;
import com.github.zxh.classpy.dexfile.body.ids.TypeIdItem;
import com.github.zxh.classpy.dexfile.list.OffsetsKnownList;
import com.github.zxh.classpy.dexfile.list.SizeKnownList;
import com.github.zxh.classpy.dexfile.list.SizeHeaderList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * The parse result of .dex file.
 * http://source.android.com/devices/tech/dalvik/dex-format.html
 * 
 * @author zxh
 */
public class DexFile extends DexComponent {
    
    private HeaderItem header;
    private SizeKnownList<StringIdItem> stringIds;
    private SizeKnownList<TypeIdItem> typeIds;
    private SizeKnownList<ProtoIdItem> protoIds;
    private SizeKnownList<FieldIdItem> fieldIds;
    private SizeKnownList<MethodIdItem> methodIds;
    private SizeKnownList<ClassDefItem> classDefs;
    private SizeHeaderList<MapItem> mapList;
    private OffsetsKnownList<StringDataItem> stringDataList;
    private OffsetsKnownList<ClassDataItem> classDataList;
    private OffsetsKnownList<SizeHeaderList<TypeItem>> typeList;

    @Override
    protected void readContent(DexReader reader) {
        readHeader(reader);
        readIds(reader);
        readClassDefs(reader);
        readData(reader);
        super.postRead(this);
    }
    
    private void readHeader(DexReader reader) {
        header = new HeaderItem();
        header.read(reader);
    }
    
    private void readIds(DexReader reader) {
        reader.setPosition(header.getStringIdsOff());
        stringIds = reader.readSizeKnownList(header.getStringIdsSize(), StringIdItem::new);
        reader.setPosition(header.getTypeIdsOff());
        typeIds = reader.readSizeKnownList(header.getTypeIdsSize(), TypeIdItem::new);
        reader.setPosition(header.getProtoIdsOff());
        protoIds = reader.readSizeKnownList(header.getProtoIdsSize(), ProtoIdItem::new);
        reader.setPosition(header.getFieldIdsOff());
        fieldIds = reader.readSizeKnownList(header.getFieldIdsSize(), FieldIdItem::new);
        reader.setPosition(header.getMethodIdsOff());
        methodIds = reader.readSizeKnownList(header.getMethodIdsSize(), MethodIdItem::new);
    }
    
    private void readClassDefs(DexReader reader) {
        reader.setPosition(header.getClassDefsOff());
        classDefs = reader.readSizeKnownList(header.getClassDefsSize(), ClassDefItem::new);
    }
    
    private void readData(DexReader reader) {
        reader.setPosition(header.getMapOff());
        mapList = reader.readSizeHeaderList(MapItem::new);
        
        reader.setPosition(stringIds.get(0).getStringDataOff());
        stringDataList = reader.readOffsetsKnownList(StringDataItem::new,
                stringIds.stream().mapToInt(stringId -> stringId.getStringDataOff().getValue()));
        
        reader.setPosition(classDefs.get(0).getClassDataOff());
        classDataList = reader.readOffsetsKnownList(ClassDataItem::new,
                classDefs.stream().mapToInt(classDef -> classDef.getClassDataOff().getValue()));
        
        // todo
        readTypeList(reader);
    }
    
    private void readTypeList(DexReader reader) {
        IntStream off1 = classDefs.stream()
                .map(ClassDefItem::getInterfacesOff)
                .mapToInt(off -> off.getValue())
                .filter(off -> off > 0);
        IntStream off2 = protoIds.stream()
                .map(ProtoIdItem::getParametersOff)
                .mapToInt(off -> off.getValue())
                .filter(off -> off > 0);
        int[] offArr = IntStream.concat(off1, off2).distinct().toArray();
        
        Supplier<SizeHeaderList<TypeItem>> factory = () -> new SizeHeaderList<>(TypeItem::new);
        
        reader.setPosition(offArr[0]);
        typeList = reader.readOffsetsKnownList(factory, Arrays.stream(offArr));
    }
    
    @Override
    public List<? extends DexComponent> getSubComponents() {
        return Arrays.asList(header,
                stringIds, typeIds, protoIds, fieldIds, methodIds, classDefs,
                mapList, stringDataList, classDataList, typeList);
    }
    
    public String getString(UInt index) {
        return getString(index.getValue());
    }
    
    public String getString(int index) {
        return stringDataList.get(index).getValue();
    }
    
    public TypeIdItem getTypeIdItem(UInt index) {
        return getTypeIdItem(index.getValue());
    }
    
    public TypeIdItem getTypeIdItem(int index) {
        return typeIds.get(index);
    }
    
    public FieldIdItem getFieldIdItem(int index) {
        return fieldIds.get(index);
    }
    
    public MethodIdItem getMethodIdItem(int index) {
        return methodIds.get(index);
    }
    
}
