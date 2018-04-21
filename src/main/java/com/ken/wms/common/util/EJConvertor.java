package com.ken.wms.common.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Excel -JavaBean 转换器
 *
 * @author Ken
 * @since 2017/3/27.
 */
public class EJConvertor {

    /**
     * 默认配置文件名
     */
    private static final String DEFAULT_CONFIG_FILE_NAME = "EJConvertorConfig.xml";

    /**
     * Entity 节点名称
     */
    private static final String ENTITY_ELEMENT = "entity";

    /**
     * Property 节点名称
     */
    private static final String PROPERTY_ELEMENT = "property";


    /**
     * Field 节点信息
     */
    private static final String FIELD_ELEMENT = "field";

    /**
     * Value 节点信息
     */
    private static final String VALUE_ELEMENT = "value";

    /**
     * class 属性
     */
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * sheetName 属性
     */
    private static final String SHEET_NAME_ATTRIBUTE = "sheetName";

    /**
     * boldHeading 属性
     */
    private static final String BOLD_HEADING_ATTRIBUTE = "boldHeading";

    /**
     * JavaBean的映射信息
     */
    private Map<String, MappingInfo> excelJavaBeanMap;

    public EJConvertor() {
        init(DEFAULT_CONFIG_FILE_NAME);
    }

    public EJConvertor(String filePath) {
        init(filePath);
    }

    /**
     * 初始化映射信息
     *
     * @param fileLocation 配置文件路径
     */
    private void init(String fileLocation) {
        try {
            // 读取配置文件
            File configFile = new ClassPathResource(fileLocation).getFile();
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = documentBuilder.parse(configFile);

            // 解析配置文件
            this.excelJavaBeanMap = parseMappingInfo(doc);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析 Document root 下配置的所有 excel-javaBean 映射信息
     *
     * @param root Document 根节点
     * @return 返回 excel-javaBean 映射信息
     */
    private Map<String, MappingInfo> parseMappingInfo(Document root) {
        // 获取 root 下的所有 entity 节点
        NodeList entities = root.getElementsByTagName(ENTITY_ELEMENT);

        // 创建承载 MappingInfo 信息的Map
        Map<String, MappingInfo> mappingInfoMap = new HashMap<>(entities.getLength());

        // 解析 entity 节点
        for (int index = 0; index < entities.getLength(); index++) {
            // 创建 mappingInfo
            MappingInfo mappingInfo = new MappingInfo();

            // 解析节点信息
            Node entityNode = entities.item(index);
            if (entityNode.getNodeType() == Node.ELEMENT_NODE) {
                Element entityElement = (Element) entityNode;
                parseEntityElement(entityElement, mappingInfo);
            }

            // 保存节点信息
            mappingInfoMap.put(mappingInfo.getClassName(), mappingInfo);
        }
        return mappingInfoMap;
    }

    /**
     * 解析 entity 节点信息
     *
     * @param entityElement entity 节点
     * @param mappingInfo   本 entity 节点包含的映射信息
     */
    private void parseEntityElement(Element entityElement, MappingInfo mappingInfo) {
        // 解析 entity 的 class 属性
        String className = entityElement.getAttribute(CLASS_ATTRIBUTE);
        mappingInfo.setClassName(className);

        // 解析 entity 的 sheetName 属性
        if (entityElement.hasAttribute(SHEET_NAME_ATTRIBUTE))
            mappingInfo.setSheetName(entityElement.getAttribute(SHEET_NAME_ATTRIBUTE));

        // 解析 entity 的 boldHeading 属性
        if (entityElement.hasAttribute(BOLD_HEADING_ATTRIBUTE)) {
            String isBoldHeading = entityElement.getAttribute(BOLD_HEADING_ATTRIBUTE);
            mappingInfo.setBoldHeading(isBoldHeading.equals("true"));
        }

        // 读取并解析 property 节点
        NodeList properties = entityElement.getElementsByTagName(PROPERTY_ELEMENT);
        for (int index = 0; index < properties.getLength(); index++) {
            Node propertyNode = properties.item(index);
            if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertyElement = (Element) propertyNode;
                parsePropertyElement(propertyElement, mappingInfo);
            }
        }
    }

    /**
     * 解析 property 节点信息
     *
     * @param propertyElement property 节点
     * @param mappingInfo     承载映射信息
     */
    private void parsePropertyElement(Element propertyElement, MappingInfo mappingInfo) {
        NodeList infoNodes = propertyElement.getChildNodes();
        String field = null;
        String value = null;

        for (int infoNode_index = 0; infoNode_index < infoNodes.getLength(); infoNode_index++) {
            Node infoNode = infoNodes.item(infoNode_index);
            if (infoNode.getNodeName().equals(FIELD_ELEMENT))
                field = infoNode.getTextContent();
            if (infoNode.getNodeName().equals(VALUE_ELEMENT))
                value = infoNode.getTextContent();
        }

        // 添加到映射信息中
        if (field != null && value != null) {
            mappingInfo.addFieldValueMapping(field, value);
            mappingInfo.addValueFieldMapping(value, field);
        }
    }

    /**
     * 讀取 Excel 文件中的内容 Excel 文件中的每一行代表了一个对象实例，而行中各列的属性值对应为对象中的各个属性值
     * 读取时，需要指定读取目标对象的类型以获得相关的映射信息，并且要求该对象已在配置文件中注册
     *
     * @param javaBeanClass 目标对象的类型
     * @param file          数据来源的 Excel 文件
     * @return 包含若干个目标对象实例的 List
     */
    public <T> List<T> excelReader(Class<T> javaBeanClass, File file) {
        // 参数检查
        if (file == null || javaBeanClass == null)
            return null;

        // 初始化存放读取结果的 List
        List<T> javaBeans = new ArrayList<>();

        // 获取类名和映射信息
        String className = javaBeanClass.getName();
        MappingInfo mappingInfo = excelJavaBeanMap.get(className);
        if (mappingInfo == null)
            return null;

        // 读取 Excel 文件
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet dataSheet = workbook.getSheetAt(0);
            Row row;
            Cell cell;

            Iterator<Row> rowIterator = dataSheet.iterator();
            Iterator<Cell> cellIterator;

            // 读取第一行表头信息
            if (!rowIterator.hasNext())
                return null;
            String fieldName;
            Field fieldInstance;
            Class<?> fieldClass;
            List<String> fieldNameList = new ArrayList<>();// 目标对象的 field 名称列表
            List<Class<?>> fieldClassList = new ArrayList<>();// 目标对象 field 类型列表
            row = rowIterator.next();
            cellIterator = row.iterator();
            while (cellIterator.hasNext()) {
                cell = cellIterator.next();

                // 获取 value 对应的 field 的名称以及类型
                fieldName = mappingInfo.getValueFieldMapping(cell.getStringCellValue());
                fieldClass = (fieldName != null && (fieldInstance = javaBeanClass.getDeclaredField(fieldName)) != null) ?
                        fieldInstance.getType() : null;

                // 保存 value 对应的 field 的名称和类型
                fieldClassList.add(cell.getColumnIndex(), fieldClass);
                fieldNameList.add(cell.getColumnIndex(), fieldName);
            }

            // 读取表格内容
            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                T javaBean = javaBeanClass.newInstance();

                // 读取单元格
                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    int columnIndex = cell.getColumnIndex();

                    // 获取单元格的值，并设置对象中对应的属性
                    Object fieldValue = getCellValue(fieldClassList.get(columnIndex), cell);
                    if (fieldValue == null) continue;
                    setField(javaBean, fieldNameList.get(columnIndex), fieldValue);
                }
                // 放入结果
                javaBeans.add(javaBean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return javaBeans;
    }

    /**
     * 将 List 中的元素对象写入到 Excel 中，其中每一个对象的一行，每一列的内容为对象的属性
     *
     * @param classType 目标对象的类型
     * @param javaBeans 数据来源的 List
     * @return 返回excel文件
     */
    public File excelWriter(Class<?> classType, List<?> javaBeans) {
        // 参数检查
        if (classType == null || javaBeans == null)
            return null;

        // 获取类名和映射信息
        String className = classType.getName();
        MappingInfo mappingInfo = excelJavaBeanMap.get(className);
        if (mappingInfo == null)
            return null;

        // 获取该 javaBean 注册需要写到 excel 的 field
        Set<String> fields = mappingInfo.getFieldValueMapping().keySet();// 注册的 field 列表
        List<String> valuesList = new ArrayList<>();// field 对应的 excel 表头 value 列表
        fields.forEach(field -> valuesList.add(mappingInfo.getFieldValueMapping(field)));

        // 创建对应的 excel 文件
        File excel = null;
        try {
            // 创建临时文件
            excel = File.createTempFile("excel", ".xlsx");
            // 创建 workBook 对象
            Workbook workbook = new XSSFWorkbook();
            // 创建 sheet 对象
            Sheet sheet = workbook.createSheet(mappingInfo.getSheetName());

            int rowIndex = 0;
            int cellIndex;
            Row row;
            Cell cell;

            // 写入第一行表头
            cellIndex = 0;
            row = sheet.createRow(rowIndex++);
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(mappingInfo.isBoldHeading());
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            for (String value : valuesList) {
                cell = row.createCell(cellIndex);
                cell.setCellValue(value);
                cellIndex++;

                // 设置样式
                cell.setCellStyle(cellStyle);
            }

            // 写入内容数据
            for (Object javaBean : javaBeans) {
                row = sheet.createRow(rowIndex++);
                cellIndex = 0;
                for (String fieldName : fields) {
                    Object value = getField(javaBean, getGetterMethodName(fieldName));
                    cell = row.createCell(cellIndex++);
                    setCellValue1(value, workbook, cell);
                }
            }

            // 调整 cell 大小
            for (int i = 0; i < valuesList.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 将 workBook 写入到 tempFile 中
            FileOutputStream outputStream = new FileOutputStream(excel);
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return excel;
    }

    /**
     * 获取 Excel 单元格中的值
     *
     * @param fieldClass JavaBean 属性字段的类型
     * @param cell       单元格
     * @param <T>        泛型类型
     * @return 返回 JavaBean 属性类型对应的值
     */
    @SuppressWarnings("unchecked")
    private <T> T getCellValue(Class<T> fieldClass, Cell cell) {

        // field 对值
        T fieldValue = null;

        if (fieldClass == int.class || fieldClass == Integer.class) {
            // convert to Integer
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Integer integer = NumberUtils.isNumber(cellValue) ? Double.valueOf(cellValue).intValue() : 0;
            fieldValue = (T) integer;
        } else if (fieldClass == long.class || fieldClass == Long.class) {
            // convert to Long
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Long l = NumberUtils.isNumber(cellValue) ? Double.valueOf(cellValue).longValue() : 0;
            fieldValue = (T) l;
        } else if (fieldClass == float.class || fieldClass == Float.class) {
            // convert to Float
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Float f = NumberUtils.isNumber(cellValue) ? Float.valueOf(cellValue) : 0;
            fieldValue = (T) f;
        } else if (fieldClass == double.class || fieldClass == Double.class) {
            // convert to Double
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Double d = NumberUtils.isNumber(cellValue) ? Double.valueOf(cellValue) : 0;
            fieldValue = (T) d;
        } else if (fieldClass == short.class || fieldClass == Short.class) {
            // convert to Short
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Short s = NumberUtils.isNumber(cellValue) ? Double.valueOf(cellValue).shortValue() : 0;
            fieldValue = (T) s;
        } else if (fieldClass == boolean.class || fieldClass == Boolean.class) {
            // get Boolean
            cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
            Boolean b = cell.getBooleanCellValue();
            fieldValue = (T) b;
        } else if (fieldClass == char.class || fieldClass == Character.class) {
            // convert to Character
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Character c = cellValue.charAt(0);
            fieldValue = (T) c;
        } else if (fieldClass == byte.class || fieldClass == Byte.class) {
            // convert to Byte
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            Byte b = NumberUtils.isNumber(cellValue) ? Double.valueOf(cellValue).byteValue() : 0;
            fieldValue = (T) b;
        } else if (fieldClass == String.class) {
            // convert to String
            cell.setCellType(Cell.CELL_TYPE_STRING);
            String cellValue = cell.getStringCellValue();
            fieldValue = (T) cellValue;
        } else if (fieldClass == Date.class) {
            // convert to java.util.Date
            fieldValue = HSSFDateUtil.isCellDateFormatted(cell) ? (T) cell.getDateCellValue() : null;
        } else if (fieldClass == java.sql.Date.class) {
            // convert to java.sql.Date
            fieldValue = null;
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                java.sql.Date date = new java.sql.Date(cell.getDateCellValue().getTime());
                fieldValue = (T) date;
            }
        }
        return fieldValue;
    }

    /**
     * 设置单元格的值
     *
     * @param cellValue 单元格的值
     * @param workbook  workbook
     * @param cell      单元格
     */
    private void setCellValue1(Object cellValue, Workbook workbook, Cell cell) {
        // 参数检查
        if (cell == null || cellValue == null || workbook == null)
            return;

        Class<?> cellValueClass = cellValue.getClass();
        if (cellValueClass == boolean.class || cellValueClass == Boolean.class) {
            cell.setCellValue((Boolean) cellValue);
        } else if (cellValueClass == char.class || cellValueClass == Character.class) {
            cell.setCellValue(String.valueOf(cellValue));
        } else if (cellValueClass == byte.class || cellValueClass == Byte.class) {
            cell.setCellValue((Byte) cellValue);
        } else if (cellValueClass == short.class || cellValueClass == Short.class) {
            cell.setCellValue((Short) cellValue);
        } else if (cellValueClass == int.class || cellValueClass == Integer.class) {
            cell.setCellValue((Integer) cellValue);
        } else if (cellValueClass == long.class || cellValueClass == Long.class) {
            cell.setCellValue((Long) cellValue);
        } else if (cellValueClass == float.class || cellValueClass == Float.class) {
            cell.setCellValue(String.valueOf(cellValue));
//            cell.setCellValue((Float) cellValue);
        } else if (cellValueClass == double.class || cellValueClass == Double.class) {
            cell.setCellValue((Double) cellValue);
        } else if (cellValueClass == String.class) {
            cell.setCellValue((String) cellValue);
        } else if (cellValueClass == Date.class) {
            Date v = (Date) cellValue;
            CellStyle cellStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy/mm/dd"));
            cell.setCellValue(v);
            cell.setCellStyle(cellStyle);
        } else if (cellValueClass == java.sql.Date.class) {
            java.sql.Date v = (java.sql.Date) cellValue;
            CellStyle cellStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy/mm/dd"));
            cell.setCellValue(v);
            cell.setCellStyle(cellStyle);
        }
    }

    /**
     * 设置 JavaBean 指定 field 的值
     *
     * @param targetObject 指定的 JavaBean 对象
     * @param fieldName    属性字段的名称
     * @param fieldValue   属性字段的值
     * @throws Exception Exception
     */
    private void setField(Object targetObject, String fieldName, Object fieldValue) throws Exception {
        // 获取对应的 setter 方法
        Class<?> targetObjectClass = targetObject.getClass();
        Class<?> fieldClass = targetObjectClass.getDeclaredField(fieldName).getType();
        Method setterMethod = targetObjectClass.getMethod(getSetterMethodName(fieldName), fieldClass);

        // 调用 setter 方法，设置 field 的值
        setterMethod.invoke(targetObject, fieldValue);
    }

    /**
     * 获取目标对象中某个属性的值，通过调用目标对象属性对应的 getter 方法，因而要求目标对象必须设置 getter 对象，否则赋值不成功
     *
     * @param targetObject 目标对象
     * @param methodName   getter 方法名
     * @return 返回该属性的值
     * @throws Exception Exception
     */
    private Object getField(Object targetObject, String methodName) throws Exception {
        // 获得 getter 方法实例
        Class<?> targetObjectType = targetObject.getClass();
        Method getterMethod = targetObjectType.getMethod(methodName);

        // 调用方法
        return getterMethod.invoke(targetObject);
    }

    /**
     * setter 方法名缓存
     */
    private Map<String, String> setterMethodNameCache = new HashMap<>(64);

    /**
     * getter 方法名缓存
     */
    private Map<String, String> getterMethodNameCache = new HashMap<>(64);

    /**
     * 构造 setter 方法的方法名
     *
     * @param fieldName 字段名
     * @return field对应的Setter方法名
     */
    private String getSetterMethodName(String fieldName) {
        // 尝试从缓存中取出, 若没有则生成再放入
        return setterMethodNameCache.computeIfAbsent(fieldName, n -> "set" + n.replaceFirst(n.substring(0, 1), n.substring(0, 1).toUpperCase()));
    }

    /**
     * 构造 getter 方法的方法名
     *
     * @param fieldName 字段名
     * @return field对应的Getter方法名
     */
    private String getGetterMethodName(String fieldName) {
        return getterMethodNameCache.computeIfAbsent(fieldName, n -> "get" + n.replaceFirst(n.substring(0, 1), n.substring(0, 1).toUpperCase()));
    }

    /**
     * Excel-JavaBean映射信息
     */
    private class MappingInfo {
        /**
         * 映射的JavaBean的全限定类名
         */
        private String className;

        /**
         * excel 表中 sheet 的名称
         */
        private String sheetName = "sheet1";

        /**
         * 表格标题加粗
         */
        private boolean boldHeading = false;

        /**
         * Field - Value 映射
         */
        private Map<String, String> fieldValueMapping = new LinkedHashMap<>();

        /**
         * Value - Field 映射
         */
        private Map<String, String> valueFieldMapping = new LinkedHashMap<>();

        /**
         * 设置映射信息的JavaBean的全称类名
         *
         * @param className JavaBean全称类名
         */
        void setClassName(String className) {
            this.className = className;
        }

        /**
         * 返回 mappingInfo 对应的 className
         *
         * @return className
         */
        String getClassName() {
            return className;
        }

        /**
         * 设置表格的 sheet 名称
         *
         * @param sheetName sheet 名称
         */
        void setSheetName(String sheetName) {
            this.sheetName = sheetName;
        }

        /**
         * 获取表格的 sheet 名称
         *
         * @return 返回表格的 sheet 名称
         */
        String getSheetName() {
            return sheetName;
        }

        /**
         * 设置表头标题是否加粗
         *
         * @param boldHeading 是否加粗
         */
        void setBoldHeading(boolean boldHeading) {
            this.boldHeading = boldHeading;
        }

        /**
         * 获取表头标题是否加粗
         *
         * @return 返回表头标题是否加粗
         */
        boolean isBoldHeading() {
            return boldHeading;
        }

        /**
         * 添加 Field - Value 映射
         *
         * @param field Field域
         * @param value Value域
         */
        void addFieldValueMapping(String field, String value) {
            fieldValueMapping.put(field, value);
        }

        /**
         * 返回指定Field映射的Value
         *
         * @param field Field域
         * @return 返回映射的Value
         */
        String getFieldValueMapping(String field) {
            return fieldValueMapping.get(field);
        }

        /**
         * 添加 Value - Field 映射
         *
         * @param value Value域
         * @param field Field域
         */
        void addValueFieldMapping(String value, String field) {
            valueFieldMapping.put(value, field);
        }

        /**
         * 返回指定的Value 映射的 Field
         *
         * @param value Value域
         * @return 返回映射的Field
         */
        String getValueFieldMapping(String value) {
            return valueFieldMapping.get(value);
        }

        /**
         * 获得 Field - Value 映射
         *
         * @return 返回Field - Value 映射
         */
        Map<String, String> getFieldValueMapping() {
            return fieldValueMapping;
        }
    }
}
