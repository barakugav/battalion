package com.ugav.battalion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.LevelBuilder;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;

class LevelSerializerXML implements LevelSerializer {

	private boolean formatPretty = false;

	void setFormatPretty(boolean formatPretty) {
		this.formatPretty = formatPretty;
	}

	private static void addValueChild(Document dom, Element parent, String tag, Object data) {
		Element e = dom.createElement(tag);
		e.appendChild(dom.createTextNode(data.toString()));
		parent.appendChild(e);
	}

	@Override
	public void levelWrite(Level level, String outpath) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.newDocument();
			Element levelElm = dom.createElement("level");

			Element shapeElm = dom.createElement("shape");
			addValueChild(dom, shapeElm, "width", Integer.toString(level.getWidth()));
			addValueChild(dom, shapeElm, "height", Integer.toString(level.getHeight()));
			levelElm.appendChild(shapeElm);

			Element tilesElm = dom.createElement("tiles");
			for (Position pos : Utils.iterable(new Position.Iterator2D(level.getWidth(), level.getHeight()))) {
				TileDesc tile = level.at(pos);
				Element tileElm = dom.createElement("tile");
				addValueChild(dom, tileElm, "x", Integer.toString(pos.xInt()));
				addValueChild(dom, tileElm, "y", Integer.toString(pos.yInt()));

				addValueChild(dom, tileElm, "terrain", tile.terrain);

				if (tile.building != null) {
					Element buildingElm = dom.createElement("building");
					addValueChild(dom, buildingElm, "type", tile.building.type);
					addValueChild(dom, buildingElm, "team", tile.building.team);
					tileElm.appendChild(buildingElm);
				}

				if (tile.unit != null) {
					UnitDesc unit = tile.unit;
					Element unitElm = dom.createElement("unit");
					addValueChild(dom, unitElm, "type", unit.type);
					addValueChild(dom, unitElm, "team", unit.team);
					if (unit.type.transportUnits) {
						UnitDesc transportedUnit = unit.getTransportedUnit();
						Element transportedUnitElm = dom.createElement("transportedUnit");
						addValueChild(dom, transportedUnitElm, "type", transportedUnit.type);
						addValueChild(dom, transportedUnitElm, "team", transportedUnit.team);
						if (transportedUnit.type.transportUnits)
							throw new IllegalArgumentException();
						unitElm.appendChild(transportedUnitElm);
					}
					tileElm.appendChild(unitElm);
				}

				tilesElm.appendChild(tileElm);
			}
			levelElm.appendChild(tilesElm);

			dom.appendChild(levelElm);

			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			if (formatPretty) {
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			}
			File outfile = new File(outpath);
			new File(outfile.getParent()).mkdirs();
			tr.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(outfile)));

		} catch (ParserConfigurationException | TransformerException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static Iterator<Node> childrenNodes(Node parent) {
		return new Iterator<>() {

			final NodeList nodes = parent.getChildNodes();
			int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < nodes.getLength();
			}

			@Override
			public Node next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return nodes.item(idx++);
			}
		};
	}

	private static Iterable<Element> childElms(Node parent) {
		List<Element> elms = new ArrayList<>();
		for (Node n : Utils.iterable(childrenNodes(parent)))
			if (n.getNodeType() == Node.ELEMENT_NODE)
				elms.add((Element) n);
		return elms;
	}

	private static Element childElm(Node parent, String tag) {
		for (Element elm : childElms(parent))
			if (tag.equals(elm.getTagName()))
				return elm;
		return null;
	}

	private static String childData(Node parent, String tag) {
		return childElm(parent, tag).getTextContent();
	}

	@Override
	public Level levelRead(String path) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder domBuilder = factory.newDocumentBuilder();
			Document dom = domBuilder.parse(path);
			Element levelElm = dom.getDocumentElement();

			Element shapeElm = childElm(levelElm, "shape");
			final int width = Integer.parseInt(childData(shapeElm, "width"));
			final int height = Integer.parseInt(childData(shapeElm, "height"));

			LevelBuilder builder = new LevelBuilder(width, height);

			for (Element tileElm : childElms(childElm(levelElm, "tiles"))) {
				int x = Integer.parseInt(childData(tileElm, "x"));
				int y = Integer.parseInt(childData(tileElm, "y"));
				Terrain terrain = Terrain.valueOf(childData(tileElm, "terrain"));

				BuildingDesc building = null;
				Element buildingElm = childElm(tileElm, "building");
				if (buildingElm != null) {
					String type = childData(buildingElm, "type");
					String team = childData(buildingElm, "team");
					building = BuildingDesc.of(Building.Type.valueOf(type), Team.valueOf(team));
				}

				UnitDesc unit = null;
				Element unitElm = childElm(tileElm, "unit");
				if (unitElm != null) {
					Unit.Type type = Unit.Type.valueOf(childData(unitElm, "type"));
					Team team = Team.valueOf(childData(unitElm, "team"));
					if (!type.transportUnits) {
						unit = UnitDesc.of(type, team);
					} else {
						Element transportedUnitElm = childElm(unitElm, "transportedUnit");
						Unit.Type transportedUnitType = Unit.Type.valueOf(childData(transportedUnitElm, "type"));
						Team transportedUnitTeam = Team.valueOf(childData(transportedUnitElm, "team"));
						if (transportedUnitType.transportUnits)
							throw new IllegalArgumentException();
						UnitDesc transportedUnit = UnitDesc.of(transportedUnitType, transportedUnitTeam);
						if (team != transportedUnitTeam)
							throw new IllegalArgumentException();
						unit = UnitDesc.transporter(type, transportedUnit);
					}
				}

				builder.setTile(Position.of(x, y), terrain, building, unit);
			}

			return builder.buildLevel();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getFileType() {
		return "xml";
	}

}
