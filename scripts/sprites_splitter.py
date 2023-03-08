from PIL import Image
import os
import argparse


def split_sprites(sprites_path, outdir, cellw, celly, margin):
    sprites = Image.open(sprites_path)
    imgwidth, imgheight = sprites.size
    rows = (imgheight - margin) // (celly + margin)
    columns = (imgwidth - margin) // (cellw + margin)
    for row in range(rows):
        for col in range(columns):
            x = margin + col * (cellw + margin)
            y = margin + row * (celly + margin)
            box = (x, y, x + cellw, y + celly)
            img = sprites.crop(box)
            img.save(os.path.join(outdir, f"img_{row}_{col}.png"))


def main():
    parser = argparse.ArgumentParser(prog="Sprites Splitter")
    parser.add_argument("-s", "--sprites", type=str,
                        required=True, help="path to the input sprites file")
    parser.add_argument("-o", "--out", type=str, required=True,
                        help="path to an output directory")
    parser.add_argument("-w", "--width", type=int, required=True,
                        help="width of each sub image within the sprites")
    parser.add_argument("-he", "--height", type=int, required=True,
                        help="height of each sub image within the sprites")
    parser.add_argument("-m", "--margin", type=int, default=1,
                        help="margin between each sub image")
    args = parser.parse_args()
    split_sprites(args.sprites, args.out, args.width, args.height, args.margin)


if __name__ == "__main__":
    main()
