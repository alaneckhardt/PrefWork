package conversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import prefwork.Attribute;
import prefwork.datasource.BasicDataSource;

public class ExportAsTHSource {

	public static void exportAsTHSource(BasicDataSource data, String outDir,
			String name) {
		Attribute[] attrs = data.getAttributes();
		String[] names = data.getAttributesNames();

		data.restartUserId();
		Integer userIdDataset;

		try {
			int count = 0;
			BufferedWriter out = new BufferedWriter(new FileWriter(outDir
					+ name + ".dat"));
			while ((userIdDataset = data.getUserId()) != null) {
				data.setFixedUserId(userIdDataset);
				data.restart();

				List<Object> rec;
				while ((rec = data.getRecord()) != null) {
					count++;
					String userId = rec.get(0).toString();
					String objectId = rec.get(1).toString();
					String rating = rec.get(2).toString();
					for (int i = 0; i < rec.size(); i++) {
						out.write(rec.get(i)+";");						
					}
					out.write("\n");
				}
			}
			out.flush();
			out.close();

			out = new BufferedWriter(new FileWriter(outDir + name + ".def"));
			out.write(count + "\n");
			out.write(names.length + "\n");
			out.write("I;" + names[0] + ";0.3\n");
			out.write("I;" + names[1] + ";0.3\n");
			out.write("R;" + names[2] + ";0.3\n");
			for (int i = 3; i < names.length; i++) {
				String type = "";
				if (attrs[i].getType() == Attribute.NOMINAL)
					type = "N";
				if (attrs[i].getType() == Attribute.NUMERICAL)
					type = "O";
				if (attrs[i].getType() == Attribute.LIST)
					type = "L";
				out.write(type+";" + names[i] + ";0.3\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
