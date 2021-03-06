package com.brick.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.brick.service.core.DataAccessor;
import com.brick.service.entity.Context;
import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * @author ranping
 * @创建日期 2010-6-17
 * @版本 V 1.0
 */
public class FileExcelUpload {

	public static final Logger log = Logger.getLogger(FileExcelUpload.class);

	// 上传成功
	private static final String SUCCESS = "Success";
	// 文件太大
	private static final String FILETOBIGERROR = "FileToBigError";
	// 文件类型不正确
	private static final String FILETYPEERROR = "FileTypeError";
	// 文件路径为空
	private static final String FILENOTFOUNDERROR = "FileNotFoundError";
	// 标题为空
	private static final String TITLEISNULLERROR = "TitleIsNullError";
	// upload.xml文件错误
	private static final String UPLOADXMLERROR = "UploadXmlError";
	// 设置上传文件最大为 50M
	private static long MAX_SIZE = 50 * 1024 * 1024;
	private static Collection<String> pictureTypes = null;
	// 允许上传格式的列表
	// "bmp","jpeg","gif","psd","png" 图片类型
	static {
		pictureTypes = new ArrayList<String>();
		pictureTypes.add("xls");
	}

	/**
	 * 
	 * @param context
	 * @param fileItem
	 *            需要文件上的部分
	 * 
	 * @return FileToBigError 文件太大 FileTypeError 文件类型不正确 FileNotFoundError
	 *         文件路径为空 TitleIsNullError 标题为空
	 */
	@SuppressWarnings("unchecked")
	public static String fileUpload(Context context, FileItem fileItem) {
	
		List errList = context.errList;
		String filePath = fileItem.getName(); // 上传路径
		// String title = (String) contextMap.get("title"); // 标题
		// String fileType = (String) contextMap.get("fileType"); // 文件类型指身份证图片等
		String type = null;

		// 不允许不选择就上传
		if ("".equals(filePath)) {
			return FILENOTFOUNDERROR;
		} else {
			type = filePath.substring(filePath.lastIndexOf(".") + 1);
		}
		// 文件是否符合类型要求
		if (!pictureTypes.contains(type.toLowerCase())) {
			return FILETYPEERROR;
		}
		// 判断文件是否过大
		if (fileItem.getSize() > MAX_SIZE) {
			return FILETOBIGERROR;
		}
		
		context.getRequest().getSession().setAttribute("fileItem",fileItem);
		return SUCCESS;
	}

	/**
	 * 
	 * @return picturePath 读取upload-config.xml文件
	 */
	public static String getUploadPath() {

		String path = null;
/*		Properties prop = new Properties();
		InputStream in = FileExcelUpload.class.getClassLoader()
				.getResourceAsStream("upload.xml");
		try {
			prop.loadFromXML(in);
			picturePath = prop.getProperty("excelPath");
		} catch (InvalidPropertiesFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(Resources.getResourceAsReader("config/upload-config.xml"));
			Element root = document.getRootElement();
			List nodes = root.elements("action");
		    for(Iterator it=nodes.iterator();it.hasNext();){
		    	Element element = (Element) it.next();
		    	Element nameElement=element.element("name");
		    	String s = nameElement.getText();
		    	if("compose".equals(s)){
		    		Element pathElement=element.element("path");
		    		path = pathElement.getText();
		    	}
		    }
		} catch (Exception e) {
			e.printStackTrace();
		} 	
		return path;
	}

	/**
	 * 保存excel文件到硬盘中
	 * @param context
	 * @param fileItem
	 * @return
	 */
	public static Long saveExcelFileToDisk(Context context, FileItem fileItem,SqlMapClient sqlMapClient) {
		String filePath = fileItem.getName();
		String type = filePath.substring(filePath.lastIndexOf(".") + 1);
		List errList = context.errList;
		Map contextMap = context.contextMap;
		String bootPath = null;
		bootPath = getUploadPath();
		//创建时间
		//Date create_time = new Date(System.currentTimeMillis()); 
		//创建人
		int s_employeeId = Integer.parseInt(String.valueOf(contextMap.get("s_employeeId")));
		//类型1
		int TYPE = 1;
		//文件存放位置
		String file_path="";
		//文件名称
		String file_name="";
		//备注
		String remark=(String) context.getRequest().getSession().getAttribute("remark");
		Long syupId = null;
		if (bootPath != null) {
			File realPath = new File(bootPath + File.separator + type);
			if (!realPath.exists())
				realPath.mkdirs();
			// 重命名图片名称
			String excelNewName = getNewFileName();
			context.contextMap.put("uploadFileName",excelNewName);
			File uploadedFile = new File(realPath.getPath() + File.separator
					+ excelNewName + "." + type);
			// 存储在数据库中的路径
			file_path = File.separator + type + File.separator
					+ excelNewName + "." + type;
			// 图片的名称
			file_name = excelNewName + "." + type;
			// Write the uploaded file to the system
			try {
				if (errList.isEmpty()) {
					fileItem.write(uploadedFile);
					contextMap.put("file_path", file_path);
					contextMap.put("file_name", file_name);
					contextMap.put("s_employeeId", s_employeeId);
					contextMap.put("type", TYPE);
					contextMap.put("remark", remark);
					contextMap.put("title", "上传的excel");
					contextMap.put("RECT_ID", "");
					
//					DataAccessor.execute("uploadPicture.create", contextMap,
//							DataAccessor.OPERATION_TYPE.INSERT);
					syupId = (Long) sqlMapClient.insert("uploadPicture.create", contextMap);
					//System.out.println(syup_id);
				}

			} catch (Exception e) {
				log.error("com.brick.util.FileExcelUpload.create"
						+ e.getMessage());
				e.printStackTrace();
				errList.add("com.brick.util.FileExcelUpload.create"
						+ e.getMessage());
			}
		}

		return syupId;
	}

	/**
	 * 对文件进行重命名
	 * 
	 * @return 
	 */
	public static String getNewFileName() {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(calendar.YEAR);
		int month = calendar.get(calendar.MONTH) + 1;
		int day = calendar.get(calendar.DATE);
		int hour = calendar.get(calendar.HOUR_OF_DAY);
		int minute = calendar.get(calendar.MINUTE);
		int second = calendar.get(calendar.SECOND);

		StringBuffer path = new StringBuffer();
		path.append(year);
		path = (month < 10) ? path.append(0).append(month) : path.append(month);
		path = (day < 10) ? path.append(0).append(day) : path.append(day);
		path = (hour < 10) ? path.append(0).append(hour) : path.append(hour);
		path = (minute < 10) ? path.append(0).append(minute) : path
				.append(minute);
		path = (second < 10) ? path.append(0).append(second) : path
				.append(second);

		// 生成十位的随机数
		for (int i = 0; i < 10; i++) {
			int random = (int) (Math.random() * 10);
			path.append(random);
		}
		return path.toString();
	}

}
