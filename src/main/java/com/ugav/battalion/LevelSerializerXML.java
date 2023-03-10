package com.ugav.battalion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Level;
import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.core.Level.TileDesc;
import com.ugav.battalion.core.Level.UnitDesc;
import com.ugav.battalion.core.LevelBuilder;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;

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
			addValueChild(dom, shapeElm, "width", Integer.toString(level.width()));
			addValueChild(dom, shapeElm, "height", Integer.toString(level.height()));
			levelElm.appendChild(shapeElm);

			Element teamsElm = dom.createElement("teams");
			for (Team team : Team.values()) {
				Element teamElm = dom.createElement("team");

				addValueChild(dom, teamElm, "color", teamWrite(team));
				int startingMoney = level.getStartingMoney(team);
				addValueChild(dom, teamElm, "startingMoney", Integer.toString(startingMoney));

				teamsElm.appendChild(teamElm);
			}
			levelElm.appendChild(teamsElm);

			Element tilesElm = dom.createElement("tiles");
			for (Iter.Int it = Cell.Iter2D.of(level.width(), level.height()); it.hasNext();) {
				int cell = it.next();
				TileDesc tile = level.at(cell);
				Element tileElm = dom.createElement("tile");
				addValueChild(dom, tileElm, "x", Integer.toString(Cell.x(cell)));
				addValueChild(dom, tileElm, "y", Integer.toString(Cell.y(cell)));

				addValueChild(dom, tileElm, "terrain", tile.terrain);

				if (tile.building != null) {
					Element buildingElm = dom.createElement("building");
					addValueChild(dom, buildingElm, "type", tile.building.type);
					addValueChild(dom, buildingElm, "team", teamWrite(tile.building.team));
					tileElm.appendChild(buildingElm);
				}

				if (tile.unit != null) {
					UnitDesc unit = tile.unit;
					Element unitElm = dom.createElement("unit");
					addValueChild(dom, unitElm, "type", unit.type);
					addValueChild(dom, unitElm, "team", teamWrite(unit.team));
					if (unit.type.transportUnits) {
						UnitDesc transportedUnit = unit.getTransportedUnit();
						Element transportedUnitElm = dom.createElement("transportedUnit");
						addValueChild(dom, transportedUnitElm, "type", transportedUnit.type);
						addValueChild(dom, transportedUnitElm, "team", teamWrite(transportedUnit.team));
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

	private static Iter<Node> childrenNodes(Node parent) {
		return new Iter<>() {

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

	private static Iter<Element> childElms(Node parent) {
		return childrenNodes(parent).filter(n -> n.getNodeType() == Node.ELEMENT_NODE).map(n -> (Element) n);
	}

	private static Element childElmMaybeNull(Node parent, String tag) {
		for (Element elm : childElms(parent).forEach())
			if (tag.equals(elm.getTagName()))
				return elm;
		return null;
	}

	private static Element childElm(Node parent, String tag) {
		Element elm = childElmMaybeNull(parent, tag);
		if (elm == null)
			throw new RuntimeException("Element not found: '" + tag + "'");
		return elm;
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

			for (Element teamElm : childElms(childElm(levelElm, "teams")).forEach()) {
				Team team = teamRead(childData(teamElm, "color"));
				int startingMoney = Integer.parseInt(childData(teamElm, "startingMoney"));
				builder.setStartingMoney(team, startingMoney);
			}

			for (Element tileElm : childElms(childElm(levelElm, "tiles")).forEach()) {
				int x = Integer.parseInt(childData(tileElm, "x"));
				int y = Integer.parseInt(childData(tileElm, "y"));
				Terrain terrain = Terrain.valueOf(childData(tileElm, "terrain"));

				BuildingDesc building = null;
				Element buildingElm = childElmMaybeNull(tileElm, "building");
				if (buildingElm != null) {
					String type = childData(buildingElm, "type");
					String team = childData(buildingElm, "team");
					building = BuildingDesc.of(Building.Type.valueOf(type), teamRead(team));
				}

				UnitDesc unit = null;
				Element unitElm = childElmMaybeNull(tileElm, "unit");
				if (unitElm != null) {
					Unit.Type type = Unit.Type.valueOf(childData(unitElm, "type"));
					Team team = teamRead(childData(unitElm, "team"));
					if (!type.transportUnits) {
						unit = UnitDesc.of(type, team);
					} else {
						Element transportedUnitElm = childElm(unitElm, "transportedUnit");
						Unit.Type transportedUnitType = Unit.Type.valueOf(childData(transportedUnitElm, "type"));
						Team transportedUnitTeam = teamRead(childData(transportedUnitElm, "team"));
						if (transportedUnitType.transportUnits)
							throw new IllegalArgumentException();
						UnitDesc transportedUnit = UnitDesc.of(transportedUnitType, transportedUnitTeam);
						if (team != transportedUnitTeam)
							throw new IllegalArgumentException();
						unit = UnitDesc.transporter(type, transportedUnit);
					}
				}

				builder.setTile(Cell.of(x, y), terrain, building, unit);
			}

			return builder.buildLevel();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Team teamRead(String s) {
		return s.equals("None") ? null : Team.valueOf(s);
	}

	private static String teamWrite(Team t) {
		return t == null ? "None" : t.toString();
	}

	@Override
	public String getFileType() {
		return "xml";
	}

}
